CREATE DATABASE IF NOT EXISTS airline_system;
USE airline_system;

-- Tables will be created automatically by the application
-- Or you can create them manually:

CREATE TABLE users (
    id VARCHAR(20) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE flights (
    id INT PRIMARY KEY AUTO_INCREMENT,
    flight_no VARCHAR(20) NOT NULL,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure VARCHAR(50) NOT NULL,
    arrival VARCHAR(50) NOT NULL,
    seats INT NOT NULL
);

CREATE TABLE passengers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL
);

CREATE TABLE bookings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    flight_no VARCHAR(20) NOT NULL,
    passenger VARCHAR(100) NOT NULL,
    seat VARCHAR(10) NOT NULL,
    booked_at VARCHAR(50) NOT NULL,
    booked_by VARCHAR(50) NOT NULL
);
select * from bookings;
select * from users;
select * from flights;
select * from passengers;