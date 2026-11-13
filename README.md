# Stock Management System

A desktop-based stock management application developed using Java AWT. It provides inventory management, stock monitoring, transaction tracking, user management, and reporting functionalities.

## Features

- Login system for Admin and Staff users
- Navigation interface to access stock management features
- Add, update, search, and delete stock items
- Record stock-in and stock-out transactions
- Adjust stock for damaged, expired, lost, and count-correction cases
- Monitor stock levels and view low-stock alerts
- View stock summary and total stock valuation
- View timestamped stock movement history
- Add users with Admin or Staff roles
- Generate and save stock reports
- Input validation and error handling
- File-based data storage for inventory, users, and transactions

## Requirements

- Java Development Kit (JDK) 8 or later
- A desktop environment that supports Java AWT
- A Java IDE such as VS Code, IntelliJ IDEA, or Eclipse (optional)

## How to Run

1. Open a terminal in the folder containing `StockManagementSystem.java`.
2. Compile the program:

   ```bash
   javac StockManagementSystem.java
   ```

3. Run the application:

   ```bash
   java StockManagementSystem
   ```

## Default Login Credentials

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Staff | `staff` | `staff123` |

## Data Files

The application stores its data in the following text files:

- `items.txt` – Stores inventory item details.
- `users.txt` – Stores user account details and roles.
- `movements.txt` – Stores stock transaction and adjustment history.
- `stock_report.txt` – Stores the generated report.

These files are created or updated automatically by the application in its working directory.

## Notes

- This project uses Java's built-in AWT and event-handling libraries.
- Sample inventory items are created automatically when `items.txt` does not exist.
- Default Admin and Staff accounts are created when no users are loaded.
- Existing inventory and user data are loaded when the application starts.
- The text files are created in the folder from which the application is launched.
- Admin permission is required for deleting items, applying stock adjustments, and adding users.
- User passwords are stored as plain text, so this project is intended for educational and demonstration purposes rather than production use.