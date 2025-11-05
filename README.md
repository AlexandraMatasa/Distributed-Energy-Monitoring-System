# DS2025_30241_Matasa_Alexandra_Assignment_1

## Overview
This project is an **Energy Management System** which allows authenticated users to monitor and manage smart energy metering devices. It is built using a microservice architecture, orchestrated with Docker and Traefik as a reverse proxy/API gateway.

The application supports two user roles:
* **Administrator**: Can perform full CRUD operations on users and devices, as well as manage user-device assignments.
* **Client**: Can log in and view all smart energy devices assigned to their account.

## System Architecture

### Frontend

* A browser-based web application built with React, which provides the system's user interface.
* It manages user interactions, provides login functionality, and renders pages based on user roles (Admin or Client).
* This component runs as the `frontend` container in Docker and is accessible to the user at `http://localhost`.


### Backend

The backend consists of the API Gateway, three distinct microservices, and their dedicated databases.

* **API Gateway (Traefik)**
    * Acts as the single entry point for all system requests, listening on port `80`.
    * Routes incoming traffic to the correct microservice based on URL paths (e.g., `/api1`, `/api2`, `/api3`).
    * Handles security by using `forwardAuth` to validate JWT tokens via the Auth Service for protected routes.


* **Microservices Layer** 
    * **Auth Management Service (`/api3`)**: Responsible for user login and registration. It stores credentials in its own `auth-db` and generates JWT tokens for authenticated users.<br><br>

    * **User Management Service (`/api1`)**: Manages all user data (ID, username, etc.). It provides full CRUD operations for user accounts and stores data in the `user-db`. <br><br>

    * **Device Management Service (`/api2`)**: Manages all smart device data (ID, name, max consumption). It provides CRUD operations for devices and handles the assignment of devices to users, storing data in the `device-db`.


* **Databases**
    * Each microservice has its own dedicated **PostgreSQL** database (`user-db`, `device-db`, `auth-db`) to ensure data isolation and loose coupling.

### Technologies Used
* Backend: Java 21 , Spring Boot (Web, Data JPA, Security) , JWT
* Frontend: React, Nginx
* Database: PostgreSQL 16

### Infrastructure: Docker / Docker Compose
* API Gateway: Traefik 3.0
* API Documentation: SpringDoc (OpenAPI)

