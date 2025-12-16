# DS2025_30241_Matasa_Alexandra_Assignment_3

## Overview
This project is an **Energy Management System** which allows authenticated users to monitor and manage smart energy metering devices. It is built using a microservice architecture with event-driven communication through RabbitMQ, orchestrated with Docker Swarm and Traefik as a reverse proxy/API gateway.

The application supports two user roles:
* **Administrator**: Can perform full CRUD operations on users and devices, manage user-device assignments, and engage in real-time chat with clients.
* **Client**: Can log in, view assigned smart energy devices, monitor real-time energy consumption, and interact with AI-driven/Rule-based Customer Support.

## System Architecture

### Frontend

* A browser-based web application built with React, which provides the system's user interface.
* It manages user interactions, provides login functionality, displays real-time overconsumption alerts, and renders pages based on user roles (Admin or Client).
* This component runs as the `frontend` container in Docker and is accessible to the user at `http://localhost`.


### Backend

The backend consists of the API Gateway, specialized microservices, and their dedicated databases.

* **API Gateway (Traefik)**
  * Acts as the single entry point for all system requests, listening on port `80`.
  * Routes incoming traffic to the correct microservice based on URL paths (e.g., `/api1`, `/api2`, `/api3`, `/api4`, `/api5`).
  * Handles security by using `forwardAuth` to validate JWT tokens via the Auth Service for protected routes.


* **Microservices Layer**
  * **Auth Management Service (`/api3`)**: Responsible for user login and registration. It stores credentials in its own `auth-db` and generates JWT tokens for authenticated users.<br><br>

  * **User Management Service (`/api1`)**: Manages all user data (ID, username, etc.). It provides full CRUD operations for user accounts and stores data in the `user-db`. <br><br>

  * **Device Management Service (`/api2`)**: Manages all smart device data (ID, name, max consumption). It provides CRUD operations for devices and handles the assignment of devices to users, storing data in the `device-db`.

  * **Monitoring Service (`/api4`)**: Deployed with 3 replicas. Processes real-time sensor measurements from smart metering devices and computes hourly energy consumption totals. It consumes sensor data from RabbitMQ queues, validates device existence, stores individual measurements, and aggregates them into hourly totals in the `monitoring-db`.

  * **Communication & Chat Service (`/api5`)**: Provides bidirectional real-time messaging using WebSockets. Includes a Rule-based Chatbot and AI-Driven Support using the Groq API (Llama 3.3 70B) to assist users with energy-related queries.


* **Databases**
  * Each microservice has its own dedicated **PostgreSQL** database (`user-db`, `device-db`, `auth-db`, `monitoring-db`) to ensure data isolation and loose coupling.


* **Message-Oriented Middleware (RabbitMQ)**
  * Acts as the message broker for asynchronous communication between microservices.
  * Uses a **Fanout Exchange** (`sync_fanout_exchange`) to broadcast synchronization events (user/device creation, deletion) to all services.
  * Uses a **Direct Exchange** (`sensor_exchange`) to route simulator data to the central `device_measurements` queue.
  * Manages dedicated queues for each service: `auth_sync_queue`, `user_sync_queue`, `device_sync_queue`, `monitoring_sync_queue`, and `device_measurements`.
  * **Websocket Exchange** routes alert and measurement notifications to the frontend.
  * Accessible via management UI at `http://localhost:15672` (credentials: `kalo/kalo`).


* **Device Data Simulator**
  * Standalone Python application that simulates smart meter readings every 10 minutes.
  * Generates realistic consumption patterns and publishes data to RabbitMQ's `sensor_exchange`.
  * Configured via `device_config.json` with the target device UUID.
  * Sends measurements in JSON format: `{"timestamp", "deviceId", "measurementValue"}`.


### Technologies Used
* Backend: Java 21 , Spring Boot (Web, Data JPA, Security) , JWT
* Frontend: React, Nginx
* Database: PostgreSQL 16
* Message Broker: RabbitMQ 3
* Producer: Python 3, pika library
* Real-time: WebSockets (Spring WebSocket & React)
* AI Integration: Groq Cloud API (Model: llama-3.3-70b-versatile).

### Infrastructure:
* Docker Swarm
* API Gateway: Traefik 3.0
* API Documentation: SpringDoc (OpenAPI)

## Accessing the Application

* **Frontend Application**:
  * `http://localhost` <br><br>

* **Traefik Dashboard** (for monitoring routes):
  * `http://localhost:8082`<br><br>

* **API Documentation (Swagger UI)**:
  * **Auth Service**: `http://localhost/api3/swagger-ui/index.html`
  * **User Service**: `http://localhost/api1/swagger-ui/index.html`
  * **Device Service**: `http://localhost/api2/swagger-ui/index.html`

## Build and Execution Considerations

* Docker Swarm: The stack must be deployed in Swarm mode to support service scaling and the {{.Task.Slot}} replica identification.
* Consistent Hashing: The Load Balancer ensures that data from the same deviceId is always routed to the same Monitoring replica to maintain state for hourly calculations.
