### Check-in webapp

A simple Spring Boot web application, with Spring Data REST to process check ins and check outs via HTTP and store them in a PostgreSQL database.

To run the application locally you need a PostgreSQL database with a user named `postgres` and password `postgres`, and a databse called `checkin`. 
If you need to, you can adjust the settings in the `application.properties`.

To start the application, use

    mvn spring-boot:run

### Related repositories:

NFC card reader: https://github.com/RefugeesCodeAT/rc-check-in-card-reader  
Frontend: https://github.com/RefugeesCodeAT/Attendance
