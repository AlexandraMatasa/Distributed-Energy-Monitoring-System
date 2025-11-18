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
from datetime import datetime

RABBITMQ_HOST = 'localhost'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'kalo'
RABBITMQ_PASS = 'kalo'

SENSOR_EXCHANGE = 'sensor_exchange'
ROUTING_KEY = 'sensor.data'

INTERVAL_MINUTES = 10


def generate_measurement(base_load):
    hour = datetime.now().hour

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

    measurement = base_load * time_multiplier * random_variation * (INTERVAL_MINUTES / 60.0)

    return round(measurement, 3)


def main():
    if len(sys.argv) < 2:
        print("Usage: python producer.py <device_id>")
        print("Example: python producer.py device-001")
        sys.exit(1)

    device_id = sys.argv[1]

    base_load = 0.5 + (1.5 * random.random())

    print("=" * 70)
    print("Device Data Producer - Energy Management System")
    print("=" * 70)
    print(f"Device ID:       {device_id}")
    print(f"Base Load:       {base_load:.3f} kWh/hour")
    print(f"Interval:        Every {INTERVAL_MINUTES} minutes")
    print(f"Exchange:        {SENSOR_EXCHANGE}")
    print(f"Routing Key:     {ROUTING_KEY}")
    print(f"RabbitMQ:        {RABBITMQ_HOST}:{RABBITMQ_PORT}")
    print("=" * 70)
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
        print("\nPress Ctrl+C to stop\n")

        count = 0

        while True:
            measurement = generate_measurement(base_load)
            timestamp = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")

            message = {
                "timestamp": timestamp,
                "deviceId": device_id,
                "measurementValue": measurement
            }

            channel.basic_publish(
                exchange=SENSOR_EXCHANGE,
                routing_key=ROUTING_KEY,
                body=json.dumps(message),
                properties=pika.BasicProperties(
                    delivery_mode=2,  # persistent
                    content_type='application/json'
                )
            )

            count += 1
            print(f"[{timestamp}] Sent #{count}: device={device_id}, value={measurement:.3f} kWh")

            # Așteptăm (pentru test: 10 secunde, pentru producție: 600 secunde = 10 min)
            time.sleep(10)  # Schimbă cu 600 pentru intervalul real de 10 minute

    except KeyboardInterrupt:
        print("\n\n" + "=" * 70)
        print(f"Producer stopped. Total measurements sent: {count}")
        print("=" * 70)
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