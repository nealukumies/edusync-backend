# EduSync Backend
EduSync Backend provides RESTful API endpoints for managing students, courses, assignments and schedules.
It is part of the EduSync study planning application made in Software Engineering Project 1 in Metropolia UAS course. 

Frontend repository: [EduSync](https://github.com/nealukumies/edusync)

API endpoints: [API guide](API.md)

JavaDoc: [JavaDoc](https://users.metropolia.fi/~neal/otp1/edusync_javadoc/)

---

## Team Members
- Nea Lukumies
- Aaro Jylhämäki
- Juhana Hänninen
- Leevi Rinnetmäki

---

## Requirements
- Java 17 or higher 
- Maven
- Docker
- MariaDB
- (Optional) An IDE such as IntelliJ IDEA
- Jenkins (for CI/CD)

---

## Jenkins
The project uses Jenkins for continuous integration and continuous deployment (CI/CD).
The Jenkins pipeline is defined in the `Jenkinsfile` located in the root directory of the project.
The pipeline includes stages for building, testing, and deploying the application. It also generates code coverage reports using JaCoCo.

---

## Environment Variables Example
```
DB_URL=jdbc:mariadb://db4free.net:3306/edusync
DB_USER=db_user_here
DB_PASSWORD=db_user_password_here
PORT=8000
```

---

## How to Run Locally
To run the backend service locally, follow these steps:
### Using an IDE:
1. Clone the repository.
2. Set the required environment variables (see example above).
3. Open the project in your preferred Java IDE (e.g., IntelliJ IDEA).
4. Compile the project using your IDE`s build tools or Maven.
5. Run the `server.Main` class to start the server.
6. The server will start listening on the specified port.
### Using Command Line:
1. Clone the repository:
2. Set the required environment variables (see example above).
3. Navigate to the project directory:
    ```
    cd edusync-backend
    ```
4. Compile the project using Maven:
   ```
   mvn clean install
   ```
5. Then run the application in command line:
   ```
   java -jar target/edusync-server.jar
   ```
   The server will start listening on the specified port.

---

## How to Run with Docker
To run the backend service using Docker, follow these steps:
1. Ensure you have Docker installed on your machine.
2. Pull the Docker image from Docker Hub:
   ```
   docker pull oonnea/otp1_edusync_backend
   ```
3. Run the Docker container with the following command and set the required environment variables:
    ```
    docker run -d -p 8000:8000 \
   -e DB_URL=jdbc:mariadb://db4free.net:3306/edusync \
   -e DB_USER=db_user_here \
   -e DB_PASSWORD=db_user_password_here \
   -e PORT=8000 \
   oonnea/otp1_edusync_backend:latest
    ```
   
By default, the server runs on `http://localhost:8000`. Adjust the `PORT` environment variable as needed.
Make sure to replace the port number if you want to use a different one.
4. The server will start listening on the specified port.

---

## Running Tests
This project uses **JUnit 5** for unit testing and **JaCoCo** for code coverage.
To run the tests, use the following Maven command:
```
mvn clean test
mvn jacoco:report
``` 
This will execute all tests and generate a code coverage report in the `target/site/jacoco` directory.
The report can be viewed by opening the `index.html` file in a web browser.
The code coverage report is also generated automatically in the Jenkins pipeline. 
Instruction coverage is 86% in the latest build (2025-10-03): [View Report](https://users.metropolia.fi/~neal/otp1/coverage/)

---