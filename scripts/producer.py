#!/usr/bin/env python3
"""
Sensor Data Generator
Usage: python producer.py <device_id>
"""

import pika
import json
import sys
import time
import random
from datetime import datetime, timedelta

RABBITMQ_HOST = 'localhost'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'kalo'
RABBITMQ_PASS = 'kalo'

SENSOR_EXCHANGE = 'sensor_exchange'
ROUTING_KEY = 'sensor.data'

MEASUREMENT_INTERVAL_MINUTES = 10
SLEEP_SECONDS = 2
START_DAYS_AGO = 7

def generate_measurement(base_load, hour):
    if 0 <= hour < 6:
        time_multiplier = 0.5 + (0.2 * random.random())
    elif 6 <= hour < 9:
        time_multiplier = 0.8 + (0.4 * random.random())
    elif 9 <= hour < 17:
        time_multiplier = 0.7 + (0.3 * random.random())
    elif 17 <= hour < 22:
        time_multiplier = 1.0 + (0.5 * random.random())
    else:
        time_multiplier = 0.6 + (0.3 * random.random())

    random_variation = 0.9 + (0.2 * random.random())

    measurement = base_load * time_multiplier * random_variation * (MEASUREMENT_INTERVAL_MINUTES / 60.0)

    return round(measurement, 3)


def main():
    if len(sys.argv) != 2:
        print("Usage: python producer.py <device_id>")
        sys.exit(1)

    device_id = sys.argv[1]
    base_load = 0.5 + (1.5 * random.random())
    current_timestamp = datetime.now() - timedelta(days=START_DAYS_AGO)

    print("=" * 80)
    print("Device Data Producer - Energy Management System")
    print("=" * 80)
    print(f"Device ID:           {device_id}")
    print(f"Base Load:           {base_load:.3f} kWh/hour")
    print(f"Starting from:       {current_timestamp.strftime('%Y-%m-%d %H:%M:%S')} ({START_DAYS_AGO} days ago)")
    print(f"Time increment:      {MEASUREMENT_INTERVAL_MINUTES} minutes per message")
    print(f"Send interval:       {SLEEP_SECONDS} seconds (real time)")
    print(f"Exchange:            {SENSOR_EXCHANGE}")
    print(f"Routing Key:         {ROUTING_KEY}")
    print(f"RabbitMQ:            {RABBITMQ_HOST}:{RABBITMQ_PORT}")
    print("=" * 80)
    print()

    try:
        print("Connecting to RabbitMQ...")
        credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
        parameters = pika.ConnectionParameters(
            host=RABBITMQ_HOST,
            port=RABBITMQ_PORT,
            credentials=credentials,
            heartbeat=600,
            blocked_connection_timeout=300
        )

        connection = pika.BlockingConnection(parameters)
        channel = connection.channel()

        channel.exchange_declare(
            exchange=SENSOR_EXCHANGE,
            exchange_type='direct',
            durable=True
        )

        print(f"Connected to RabbitMQ")
        print(f"Exchange '{SENSOR_EXCHANGE}' ready")
        print("\nPress Ctrl+C to stop")
        print("=" * 80)
        print()

        count = 0

        while True:
            measurement = generate_measurement(base_load, current_timestamp.hour)

            message = {
                "timestamp": current_timestamp.strftime("%Y-%m-%dT%H:%M:%S"),
                "deviceId": device_id,
                "measurementValue": measurement
            }

            channel.basic_publish(
                exchange=SENSOR_EXCHANGE,
                routing_key=ROUTING_KEY,
                body=json.dumps(message),
                properties=pika.BasicProperties(
                    delivery_mode=2,
                    content_type='application/json'
                )
            )

            count += 1
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Sent #{count}: "
                  f"ts={current_timestamp.strftime('%Y-%m-%d %H:%M:%S')}, "
                  f"value={measurement:.3f} kWh")

            current_timestamp += timedelta(minutes=MEASUREMENT_INTERVAL_MINUTES)

            time.sleep(SLEEP_SECONDS)

    except KeyboardInterrupt:
        print("\n\n" + "=" * 80)
        print(f"Producer stopped. Total measurements sent: {count}")
        print(f"Final timestamp: {current_timestamp.strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 80)
    except Exception as e:
        print(f"\nError: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        if connection and not connection.is_closed:
            connection.close()
            print("Connection closed")


if __name__ == "__main__":
    main()