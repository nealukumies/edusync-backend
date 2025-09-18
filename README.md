# EduSync Backend
This is the backend service for the EduSync application that is the Software Engineering Project 1 in Metropolia UAS course. The purpose of this project is to use agile software development methods to create a software product for study planning.
Frontend repository: ["EduSync"](https://github.com/nealukumies/edusync)

This backend service is build using HttpServer from `com.sun.net.httpserver` package. The service provides RESTful API endpoints for managing students, courses, assignments and schedules.

## Endpoints

### Students
#### GET /students
**GET** `/students/{studentId}` - Get a specific student by ID

**Authorization**: Student can access only their own data (student_id in header). Admin can access any student's data.

**Response 200:**
```json
{
  "id": "studentId",
  "name": "John",
  "email": "johns@mail.com"
}
```
**Errors:**
- 404 Not Found: Student with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the student's data.


#### POST /students
**POST** `/students` - Create a new student

**Request Body:**
```json
{
  "name": "Alice",
  "email": "alices@mail.com",
  "password": "password"
}
```
**Response 201:**
```json
{
  "id": "1",
  "name": "Alice",
  "email": "alices@mail.com"
}
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 409 Conflict: Email already exists.


#### PUT /students
**PUT** `/students/{studentId}` - Update a specific student by ID

**Authorization**: Student can update only their own data. Admin can update any student's data.

**Request Body:**
```json
{
    "name": "New Alice",
    "email": "new@mail.fi"
}
```
**Response 200:**
```json
{
  "id": "studentId",
  "name": "New Alice",
  "email": "new@mail.fi"
}
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 404 Not Found: Student with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the student's data.
- 409 Conflict: Email already exists.


#### DELETE /students
**DELETE** `/students/{studentId}` - Delete a specific student by ID

**Authorization**: Student can delete only their own account. Admin can delete any student's account.

**Response 200:**
```json
{
  "message": "Student deleted successfully"
}
```
**Errors:**
- 404 Not Found: Student with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to delete the student's account.


### Courses
#### GET /courses
**GET** `/courses/{courseId}` - Get a specific course by ID

**Authorization**: Student can access only their own courses. Admin can access any course.

**Response 200:**
```json
{"course_id": "courseId",
  "student_id": "studentId",
  "course_name": "Mathematics",
  "start_date": "2023-01-01",
  "end_date": "2023-06-01"
}
```
**Errors:**
- 404 Not Found: Course with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the course data.

**GET** `/courses/students/{studentId}` - Get all courses for a specific student

**Authorization**: Student can access only their own courses. Admin can access any student's courses.

**Response 200:**
```json
[
  {"course_id": "1",
  "student_id": "studentId",
  "course_name": "Mathematics",
  "start_date": "2025-01-01",
  "end_date": "2025-06-01"
  },
  {"course_id": "2",
  "student_id": "studentId",
  "course_name": "Physics",
  "start_date": "2025-01-01",
  "end_date": "2025-06-01"
  }
]
```
**Errors:**
- 404 Not Found: Student with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the student's courses.


#### POST /courses
**POST** `/courses` - Create a new course

**Authorization**: Requires valid student ID in header.

**Request Body:**
```json
{
  "course_name": "Mathematics",
  "start_date": "2023-01-01",
  "end_date": "2023-06-01"
}
```
**Response 201:**
```json
{
  "course_id": "1",
  "student_id": "studentId",
  "course_name": "Mathematics",
  "start_date": "2023-01-01",
  "end_date": "2023-06-01"
}
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 500 Internal Server Error: Failed to create the course.


#### PUT /courses
**PUT** `/courses/{courseId}` - Update a specific course by ID

**Authorization**: Student can update only their own courses. Admin can update any course.

**Request Body:**
```json
{
  "course_name": "Advanced Mathematics",
  "start_date": "2025-02-01",
  "end_date": "2025-07-01"
}
```
**Response 200:**
```json
{
  "course_id": "1",
  "student_id": "studentId",
  "course_name": "Advanced Mathematics",
  "start_date": "2025-02-01",
  "end_date": "2025-07-01"
}
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 404 Not Found: Course with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the course data.
- 500 Internal Server Error: Failed to update the course.


#### DELETE /courses
**DELETE** `/courses/{courseId}` - Delete a specific course by ID

**Authorization**: Student can delete only their own courses. Admin can delete any course.

**Response 200:**
```json
{
    "message": "Course deleted successfully"
}
```
**Errors:**
- 404 Not Found: Course with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to delete the course.
- 500 Internal Server Error: Failed to delete the course.


### Schedules
#### GET /schedules
**GET** `/schedules/{scheduleId}` - Get a specific schedule by ID

**Authorization**: Student can access only their own schedules. Admin can access any schedule.

**Response 200:**
```json
{
  "schedule_id": "1",
  "course_id": "1",
  "weekday": "monday",
  "start_time": "10:00",
    "end_time": "12:00"
}
```
**Errors:**
- 404 Not Found: Schedule with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the schedule data.


**GET** `/schedules/courses/{courseId}` - Get all schedules for a specific course

**Authorization**: Student can access only their own course schedules. Admin can access any course's schedules.

**Response 200:**
```json
[
  {"schedule_id": "1",
  "course_id": "1",
  "weekday": "monday",
  "start_time": "10:00",
    "end_time": "12:00"
  },
  {"schedule_id": "2",
  "course_id": "2",
  "weekday": "wednesday",
  "start_time": "14:00",
    "end_time": "16:00"
  }
]
```
**Errors:**
- 404 Not Found: Course with the specified ID does not exist.


**GET** `/schedules/students/{studentId}` - Get all schedules for a specific student

**Authorization**: Student can access only their own schedules. Admin can access any student's schedules.

**Response 200:**
```json
[
  {"schedule_id": "1",
  "course_id": "1",
  "weekday": "monday",
  "start_time": "10:00",
    "end_time": "12:00"
    },
    {"schedule_id": "2",
    "course_id": "2",
    "weekday": "wednesday",
    "start_time": "14:00",
    "end_time": "16:00"
    }
]
```
**Errors:**
- 404 Not Found: Student with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the student's schedules.


#### POST /schedules
**POST** `/schedules` - Create a new schedule

**Authorization**: Requires valid student ID in header.

**Request Body:**
```json
{
  "course_id": "1",
  "weekday": "monday",
  "start_time": "10:00",
  "end_time": "12:00"
}
```
**Response 201:**
```json
{
  "schedule_id": "1",
  "course_id": "1",
  "weekday": "monday",
  "start_time": "10:00",
  "end_time": "12:00"
 } 
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 500 Internal Server Error: Failed to create the schedule.


#### PUT /schedules
**PUT** `/schedules/{scheduleId}` - Update a specific schedule by ID

**Authorization**: Student can update only their own course schedules. Admin can update any schedule.

**Request Body:**
```json
{
  "course_id": "1",
  "weekday": "tuesday",
  "start_time": "11:00",
  "end_time": "13:00"
}
```
**Response 200:**
```json
{
  "schedule_id": "1",
  "course_id": "1",
  "weekday": "tuesday",
  "start_time": "11:00",
  "end_time": "13:00"
}
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 404 Not Found: Schedule with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the schedule data.
- 500 Internal Server Error: Failed to update the schedule.


#### DELETE /schedules
**DELETE** `/schedules/{scheduleId}` - Delete a specific schedule by ID

**Authorization**: Student can delete only their own course schedules. Admin can delete any schedule.

**Response 200:**
```json
{
    "message": "Schedule deleted successfully"
}
```
**Errors:**
- 404 Not Found: Schedule with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to delete the schedule.
- 500 Internal Server Error: Failed to delete the schedule.


### Assignments
#### GET /assignments
**GET** `/assignments/{assignmentId}` - Get a specific assignment by ID

**Authorization**: Student can access only their own assignments. Admin can access any assignment.

**Response 200:**
```json
{
  "assignment_id": "1",
  "student_id": "1",
  "course_id": "1",
  "title": "Homework 1",
  "description": "Solve problems 1-10",
  "deadline": "2025-10-01"
}
```
**Errors:**
- 404 Not Found: Assignment with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the assignment data.


**GET** `/assignments/students/{studentId}` - Get all assignments for a specific student

**Authorization:** Student can access only their own assignments. Admin can access any student's assignments.

**Response 200:**
```json
[
    {
    "assignment_id": "1",
    "student_id": "1",
    "course_id": "1",
    "title": "Homework 1",
    "description": "Solve problems 1-10",
    "deadline": "2025-10-01"
    },
    {
    "assignment_id": "2",
    "student_id": "1",
    "course_id": "2",
    "title": "Lab Report",
    "description": "Write a report on the lab experiment",
    "deadline": "2025-10-05"
    }
]
```
**Errors:**
- 404 Not Found: Student with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to the student's assignments.
- 500 Internal Server Error: Failed to retrieve assignments.


#### POST /assignments
**POST** `/assignments` - Create a new assignment

**Authorization:** Requires valid student ID in header.

**Request Body:**
```json
{
    "course_id": "1",
    "title": "Homework 1",
    "description": "Solve problems 1-10",
    "deadline": "2025-10-01"
}
```
**Response 201:**
```json
{
  "assignment_id": "1",
  "student_id": "1",
  "course_id": "1",
  "title": "Homework 1",
  "description": "Solve problems 1-10",
  "deadline": "2025-10-01"
}
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 403 Forbidden: Unauthorized access to create the assignment.
- 500 Internal Server Error: Failed to create the assignment.


#### PUT /assignments
**PUT** `/assignments/{assignmentId}` - Update a specific assignment by ID

**Authorization:** Student can update only their own assignments. Admin can update any assignment.

**Request Body: At least one field can be updated**
```json
{
    "course_id": "1",
    "title": "Updated Homework 1",
    "description": "Solve problems 1-20",
    "deadline": "2025-10-10",
    "status": "completed"
}       
```
**Response 200:**
```json
{
    "assignment_id": "1",
    "student_id": "1",
    "course_id": "1",
    "title": "Updated Homework 1",
    "description": "Solve problems 1-20",
    "deadline": "2025-10-10",
    "status": "completed"
}
```
**Errors:**
- 400 Bad Request: Missing or invalid fields in the request body.
- 403 Forbidden: Unauthorized access to the assignment data.
- 404 Not Found: Assignment with the specified ID does not exist.
- 500 Internal Server Error: Failed to update the assignment.


#### DELETE /assignments
**DELETE** `/assignments/{assignmentId}` - Delete a specific assignment by ID

**Authorization**: Student can delete only their own assignments. Admin can delete any assignment.

**Response 200:**
```json
    
{
    "message": "Assignment deleted successfully"
}
```
**Errors:**
- 404 Not Found: Assignment with the specified ID does not exist.
- 403 Forbidden: Unauthorized access to delete the assignment.
- 500 Internal Server Error: Failed to delete the assignment.