# FinTry

FinTry is a virtual investment and financial market simulation platform designed to help users explore financial instruments, track market movements, and practice investment strategies without risking real money.

The project focuses on financial data accuracy, secure transactions, portfolio management, authentication, and modern web technologies.

FinTry is currently being developed as a solo software engineering project.

## Tech Stack

### Frontend
- React 19
- JavaScript
- Vite
- CSS

### Backend
- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA / Hibernate
- JWT Authentication
- PostgreSQL

### Infrastructure
- Docker
- Flyway
- Maven
- GitHub Actions
- Testcontainers

## Features

Implemented, ongoing, and planned features include:

- User registration and authentication
- JWT-based authorization
- USER and ADMIN roles
- Virtual investment accounts
- Virtual balance management
- Financial instrument management
- Stock market tracking
- Currency exchange monitoring
- Financial market dashboard
- Historical price charts
- Virtual buying and selling
- Portfolio management
- Transaction history
- Profit and loss calculations
- Investment funds
- Bonds and treasury bills
- Futures and options (VİOP)
- Financial news
- Watchlists
- Price alerts
- Market data synchronization

## Project Structure

```text
src/
  main/
    java/
      com/fintry/
        config/
        controller/
        dto/
        entity/
        exception/
        repository/
        security/
        service/
        tools/
    resources/
      db/
        migration/
  test/
    java/
      com/fintry/

frontend/
  src/
    components/
    pages/
    test/

docs/

.github/
  workflows/
```

## Financial Transaction Architecture

The financial core is designed to preserve data integrity and transaction consistency.

The system focuses on:

- PostgreSQL transactional operations
- Concurrency-safe balance updates
- Financial precision using BigDecimal
- Database constraints and validation
- Atomic virtual buying and selling
- Portfolio ownership verification
- Prevention of negative balances and invalid prices
- Protection against duplicate portfolio positions

Financial transactions are processed through the backend, with validation and authorization enforced on the server.

## Security Architecture

FinTry uses Spring Security and JWT-based authentication.

The security system includes:

- Secure user registration and login
- BCrypt password hashing
- Stateless JWT access tokens
- USER and ADMIN role authorization
- Server-side resource ownership checks
- Protected financial operations
- Restricted administrative endpoints
- Authentication rate limiting
- Secure database credential configuration

A dedicated PostgreSQL application role, `fintry_app`, is used to separate runtime permissions from database administration and migration privileges.

## Development

### Requirements
- Java 21
- Maven Wrapper
- Node.js 22+
- npm
- PostgreSQL 18

### Backend

Run backend tests:

```bash
./mvnw clean test
```

Start the backend:

```bash
./mvnw spring-boot:run
```

### Frontend

Navigate to the frontend directory:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

**Note:** Running the application requires the appropriate database schema, environment configuration, and authentication secrets. Database migrations must be completed before starting the backend against an existing database.

## Development Workflow

The project is developed incrementally using feature branches.

Main development flow:

```text
feature/* -> develop -> main
```

Each development milestone includes:

- Backend and frontend implementation
- Database migration review
- Automated tests
- Security and authorization checks
- Financial data integrity verification
- Regression testing
- Documentation updates
- Git commit and push
- Pull request review

## Developer

**Faruk Turnalı**

Software Engineering Student  
Full-Stack Development & Financial Software Systems

## Project Status

Under active development.

The financial transaction core, JWT authentication, authorization, and database security foundations have been implemented.

Current development focuses on database migration, local application integration, market data processing, financial dashboards, portfolio functionality, virtual trading, financial instruments, news, alerts, testing, and deployment.

FinTry is intended for educational and simulation purposes. It does not execute real-money transactions or provide personalized investment advice.
