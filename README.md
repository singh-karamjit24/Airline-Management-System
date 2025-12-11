✈️ Airline Management System (Java + Swing + MySQL)

This project is a Java-based Airline Management System that integrates a MySQL database with a Swing GUI interface. The system supports authentication, role-based access, flight management, passenger booking, and real-time database operations through JDBC. It automatically creates required tables and initializes a default admin account on first launch.

🌟 Key Features
🔐 User Authentication

Login system with username & password

Role-based access control (Admin / Staff)

🛫 Flight Management Module

Add new flights (flight number, origin, destination, departure, arrival, seat capacity)

Edit / update existing flight records

Delete flights

View all flight entries in a dynamic JTable

Stores all data in MySQL

🎟️ Booking Management

Create new bookings for registered flights

Select seat number & passenger details

Stores booking date/time automatically

View all bookings in a table

Delete bookings if necessary

🗄️ MySQL Database Integration

Uses JDBC for all CRUD operations

Automatically creates required tables:

users

flights

bookings

Inserts a default admin account on first run

Centralized DatabaseManager handles all SQL queries

💻 Modern Java Swing GUI

Stylish, responsive UI using:

JFrame, JPanel, JButton, JTable, JTextField, etc.

Custom utility for themed buttons

Multiple windows for Login, Dashboard, Flight Management, Booking Management

Clean navigation with tabbed panels

🔧 Utility Modules in Code

DBConfig for database connection settings

DatabaseManager for initialization and CRUD operations

Inner classes for UI: LoginFrame, DashboardFrame, Dialogs, etc.

Uses DefaultTableModel for dynamic table updates

📚 Technology Stack

Java (JDK 8 or above)

Java Swing (GUI)

JDBC (MySQL Connector)

MySQL Database

Collections, OOP, Exception Handling

📂 Project Structure (Based on Your Code)
AirlineSystemWithDB.java
└── DBConfig (Database connection details)
└── DatabaseManager (CRUD + schema creation)
└── Flight / Booking Models
└── LoginFrame (User login window)
└── DashboardFrame (Main admin/staff UI)
└── Flight Management Panel
└── Booking Management Panel
└── Reusable UI utilities (Styled buttons, dialogs)

🏁 How to Run the Project
1️⃣ Set up MySQL database

Create a database:

CREATE DATABASE airline_system;

2️⃣ Update DB credentials

Inside DBConfig:

public static final String URL = "jdbc:mysql://localhost:3306/airline_system";
public static final String USER = "root";
public static final String PASSWORD = "your_password";

3️⃣ Add MySQL JDBC Driver

Download and add:

mysql-connector-j.jar


to your project classpath.

4️⃣ Compile and Run
javac AirlineSystemWithDB.java
java AirlineSystemWithDB


The system will:

Test DB connection

Create all tables automatically

Insert default admin user:

Username: admin

Password: admin

🚀 Future Enhancements

PDF ticket generation

Search & filter flights

Passenger database

Real-time seat map

Airline staff management module

JavaFX UI upgrade
