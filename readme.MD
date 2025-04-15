# TSD Planning Poker

## Project Overview
TSD Planning Poker is a collaborative tool designed to facilitate agile planning and estimation. This project is developed as part of the "Technologie Rozwoju Oprogramowania" course.

## Team Members
- Mikołaj Diakowski

---

## Prerequisites
To develop and run this application, you will need the following tools installed on your machine:
- **Docker**: For containerization and running services.
- **Docker Compose**: To manage multi-container Docker applications.
- **Node.js**: For running the frontend and backend development environment.
- **npm**: For managing JavaScript dependencies.
- **Git**: For version control.

---

## Project Setup
Follow these steps to configure the project on your development machine:

1. **Clone the Repository**:
    ```bash
    cd tsd-planning-poker
    ```

2. **Start Services with Docker Compose**:
    Run the following command to start the database, web server, and other services:
    ```bash
    docker-compose up --build
    ```