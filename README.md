# Payment Labeling Engine

A Spring Boot application for automatically labeling and aggregating payment transactions from CSV files.

## Technologies

- **Java**: 21 LTS
- **Spring Boot**: 3.3.0
- **Database**: PostgreSQL
- **Template Engine**: Thymeleaf
- **Database Migration**: Flyway
- **Build Tool**: Maven

## Features

- Import payment data from CSV files (Slovak bank statement format)
- Automatic labeling based on regex rules
- Manual labeling interface
- Payment aggregation by label, month, and year
- Responsive web UI with Thymeleaf and Bootstrap
- Duplicate detection for overlapping CSV imports

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 12+

## Setup & Installation

### 1. Clone the repository

```bash
git clone https://github.com/Ladislav-Luyi/payment-labeling-engine.git
cd payment-labeling-engine
```

### 2. Create PostgreSQL database

```bash
psql -U postgres
CREATE DATABASE payment_labeling_db;
\q
```

### 3. Configure Database Connection

Edit `src/main/resources/application.yml` and update PostgreSQL credentials if needed:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payment_labeling_db
    username: postgres
    password: postgres
```

### 4. Build the project

```bash
mvn clean install
```

### 5. Run the application

```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## Project Structure

```
src/
├── main/
│   ├── java/com/paymentlabeling/
│   │   ├── controller/          # Web controllers
│   │   ├── model/               # JPA entities
│   │   ├── repository/          # Data access layer
│   │   ├── service/             # Business logic
│   │   └── PaymentLabelingEngineApplication.java  # Main class
│   └── resources/
│       ├── db/migration/        # Flyway migrations
│       ├── templates/           # Thymeleaf templates
│       └── application.yml      # Configuration
└── test/                        # Unit and integration tests
```

## Database Schema

The application uses the following main entities:

- **Payment**: Bank transactions
- **Label**: Labels for categorization
- **LabelingRule**: Regex-based rules for automatic labeling
- **PaymentLabel**: Many-to-many relationship between payments and labels
- **Aggregate**: Aggregated data by label, month, and year

See Issue #2 for the complete database schema implementation.

## Development

### Running Tests

```bash
mvn test
```

### Building JAR

```bash
mvn clean package
```

The JAR file will be available in `target/` directory.

## API Endpoints

(To be documented after controller implementation)

## Contributing

1. Create a new branch for your feature
2. Commit your changes
3. Push to the repository
4. Create a Pull Request

## Issues & Roadmap

Development roadmap is tracked in GitHub Issues:
1. Project Setup & Infrastructure (✅ In Progress)
2. Database Schema Design
3. CSV Parser Implementation
4. Automatic Labeling Engine
5. Manual Labeling Interface
6. Aggregate & Reporting System
7. User Interface - Navigation & Layout
8. Testing & Documentation

## License

This project is licensed under the MIT License.