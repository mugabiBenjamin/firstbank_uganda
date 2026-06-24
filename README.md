# First Bank Uganda - Account Opening Application

A modern, professional **desktop JavaFX application** for opening new bank accounts at First Bank Uganda. The application provides a clean user interface for collecting customer information, performs comprehensive validation, generates unique account numbers, and persists records to a Microsoft Access (`.accdb`) database.

![Java](https://img.shields.io/badge/Java-21-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.9-green)
![UCanAccess](https://img.shields.io/badge/Database-MS_Access-0078D4)

- [First Bank Uganda - Account Opening Application](#first-bank-uganda---account-opening-application)
  - [Features](#features)
  - [Technologies](#technologies)
  - [Prerequisites](#prerequisites)
  - [Quick Start](#quick-start)
    - [1. Clone the Repository](#1-clone-the-repository)
    - [2. Build the Project](#2-build-the-project)
    - [3. Run the Application](#3-run-the-application)
  - [Project Structure](#project-structure)
  - [Database](#database)
  - [Configuration](#configuration)
  - [Usage Guide](#usage-guide)
    - [Opening a New Account](#opening-a-new-account)
    - [Account Lookup](#account-lookup)
    - [Export PDF](#export-pdf)
  - [Architecture](#architecture)
  - [Security Features](#security-features)
  - [License](#license)
  - [Contributing](#contributing)

## Features

- **Intuitive Form Interface**: Clean, responsive form with real-time validation feedback
- **Comprehensive Validation**:
  - Name format (letters only)
  - Ugandan NIN validation (CM/CF + 8 digits + 4 letters)
  - Email and phone number format (+256XXXXXXXXX)
  - PIN strength (4-6 digits, not all identical)
  - Age requirements per account type
  - Minimum opening deposit checks
- **Account Types**:
  - Savings (UGX 50,000 min)
  - Current (UGX 200,000 min)
  - Fixed Deposit (UGX 1,000,000 min)
  - Student (UGX 10,000 min, age 18-25)
  - Joint (requires second NIN)
- **Unique Account Number Generation**: Format `KLA-2026-000142` with branch-specific sequencing
- **Secure PIN Handling**: bcrypt hashing (never stored in plain text)
- **Duplicate Detection**: Prevents multiple accounts with same NIN + type + branch
- **Account Lookup**: Search by Account Number or NIN
- **PDF Export**: Professional confirmation documents with bank branding
- **Modern UI**: Dark header, clean cards, responsive layout with custom CSS

## Technologies

- **Backend**: Java 21, Maven
- **UI**: JavaFX 21
- **Database**: Microsoft Access (`.accdb`) via **UCanAccess** JDBC driver
- **Security**: bcrypt (0.10.2)
- **PDF Generation**: iText 5.5.13.4
- **Build Tool**: Maven

## Prerequisites

- **Java 21** (JDK 21)
- **Maven 3.8+**
- Windows (recommended for MS Access compatibility) or Linux/macOS with proper JDBC setup

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd firstbank_uganda
```

### 2. Build the Project

```bash
mvn clean package
```

### 3. Run the Application

```bash
mvn clean javafx:run
# Or run the generated JAR:
java -jar target/firstbank_uganda-1.0.0.jar

```

## Project Structure

```plain
firstbank_uganda/
├── data/                          # Database location (firstbank.accdb)
├── src/main/java/ug/firstbank/
│   ├── MainApp.java              # Application entry point
│   ├── model/                    # Account type hierarchy
│   ├── persistence/              # Database access layer
│   ├── service/                  # Business logic
│   ├── ui/                       # JavaFX UI components
│   ├── util/                     # Utilities (PDF, formatting, etc.)
│   └── validation/               # Form validation
├── src/main/resources/
│   └── ug/firstbank/ui/theme.css # Application styling
├── pom.xml
├── .gitignore
├── .gitattributes
└── README.md
```

## Database

The application uses a Microsoft Access database (data/firstbank.accdb).
Tables:

- `accounts` - Stores all customer account records
- `account_seq` - Manages per-branch, per-year sequence numbers

The database is automatically created on first run with proper schema.

## Configuration

Database path can be customized via system property:

```bash
-Dapp.db.path=/path/to/firstbank.accdb
```

Configured in `pom.xml` for Maven JavaFX plugin.

## Usage Guide

### Opening a New Account

1. Fill in personal details (Name, NIN, Email, Phone, DOB, PIN)
2. Select Account Type and Branch
3. Enter Opening Deposit
4. Click Submit Application
5. Review summary and export PDF

### Account Lookup

- Menu → Account → Find Account…
- Search by Account Number or NIN

### Export PDF

After successful submission, click Export to PDF in the summary pane
Generates a branded, professional confirmation document

## Architecture

- **Model**: Abstract `Account` hierarchy with specific rules per type
- **Persistence**: Repository pattern with transaction support
- **Service**: Orchestrates validation, duplicate checks, and persistence
- **UI**: Clean separation with form snapshot pattern
- **Validation**: Centralized, reusable validation with clear error messages

## Security Features

- PINs are never stored in plaintext (bcrypt hashed)
- Input sanitization and strict validation
- Transactional database operations with rollback support
- Prepared statements to prevent SQL injection

## License

This project is for internal First Bank Uganda use. All rights reserved.

## Contributing

For internal development:

1. Follow existing code style
2. Update tests when adding features
3. Ensure database schema changes are handled in DatabaseManager

---

**First Bank Uganda** - Your Trusted Banking Partner
© 2026 First Bank Uganda. All Rights Reserved.

**Ready to use** - Copy the above content into a new `README.md` file at the project root. It includes all key information about the application in a clean, professional format.

[Back to Top](#first-bank-uganda---account-opening-application)
