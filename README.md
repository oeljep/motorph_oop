# Payroll Hub

Payroll Hub is a Java 17 Swing payroll application for MotorPH. The GUI is connected to a local CSV data layer: employee, account, attendance, and leave operations use `CsvHandler`; attendance writes are validated by `AttendanceService`; and payslip views use `PayslipCalculator` and `PayrollCalculator`.

> **MO-IT110 Object-Oriented Programming · Milestone 2**
>
> Mapúa-Malayan Digital College · Section S2101
> Rowel Jepsani

## Current implementation status

| Area | Current status |
|---|---|
| Sign-in and role-based navigation | Implemented. `UserService` authenticates against `users.csv`; `NavAccess` controls which dashboard tabs are available. |
| Dashboard overview | Implemented. Management roles see employee headcount, basic payroll totals, average salary, pending leave count, and an employee table; staff see their own employment, pay, allowance, attendance, and pending-leave summaries. |
| Employee records | Implemented. Search, view, add, edit, and delete screens use `CsvHandler`; add/delete and edit controls vary by role. |
| Time tracker | Implemented. Employees can clock in/out and review their own attendance history. `AttendanceService` writes records; `AttendanceRules` supplies late-minute and paid-hour calculations. |
| Leave requests | Implemented. Users can file and view requests; authorized Admin roles can view and decide leave requests for their direct reports only. Data is persisted in `leaves.csv`. |
| Payslip and payroll screens | Connected to employee and attendance data. The payroll calculation rules and attendance/pay assumptions still require review against the course/source payroll policy before treating the output as production-verified. |
| Ticket manager | Implemented in the GUI/data layer. Tickets are persisted to `src/data/tickets.csv`, created on first filing; replies and status updates are saved. |
| Password recovery | Implemented through the login recovery flow and `UserService`. It is not an IT staff dashboard. |
| IT operations and overtime approval | Not currently available as dashboard workflows. Overtime is shown as requiring supervisor approval; no approval action is implemented. |

The current source was verified with a Java 17 compile and the read-only `GuiBackendIntegrationTest`. The existing legacy test mains still contain failures in employee-data assertions and payroll rule expectations; those are not represented as passing. Maven was unavailable in the validation environment, so `mvn test` was not run.

## Features

- Role-based sign-in and dashboard tabs.
- Role-specific management and employee dashboard summaries.
- Employee search, profile viewing, and role-limited add/edit/delete actions.
- Employee self-service clock in/out and monthly attendance history.
- Leave filing, calendar display, and direct-report-scoped approval/rejection for Admin only.
- Employee payslip display and accounting payroll table/export.
- Ticket filing from payslip concerns and ticket response/status handling.
- Password recovery using account and employee identity details.

## Dashboard access

Every recognized role gets Dashboard, Time Tracker, Schedule, and Payslip tabs. `NavAccess` additionally exposes:

| Role in `users.csv` | Additional tabs |
|---|---|
| `Admin` | Employee Manager, Leave Manager |
| `HumanResources` | Employee Manager |
| `Accounting` | Payroll Manager, Dispute Manager |
| `Regular` | No management tabs |
| `InfoTech` | Ticket Manager (handles access support issue) |

## OOP design

The project’s CRC cards and Method Dictionary are maintained in [`src/docs/crc-method-dictionary.csv`](./src/docs/crc-method-dictionary.csv); the CSV layouts and field rules are in [`src/docs/data-dictionary.md`](./src/docs/data-dictionary.md).

| Pillar | Current application |
|---|---|
| **Encapsulation** | Employee and record data are represented by model/data classes; validation is performed by their constructors/setters and service/data operations. |
| **Abstraction** | `Employee` is abstract, and role/service contracts are defined in interfaces such as `UserHub`, `AdminHub`, `HRHub`, `FinanceHub`, `SystemSupportHub`, `LeaveHub`, and `PayrollProcessor`. |
| **Inheritance** | Role models including `Admin`, `HumanResources`, `Accounting`, `InfoTech`, and `Regular` derive from `Employee`. |
| **Polymorphism** | Role/model implementations provide payroll and workflow behavior through overrides and overloaded contract methods. The GUI itself uses role-based `NavAccess` checks rather than relying on `instanceof` checks for tab permissions. |

## Project structure

```text
src/
├── data/                         # employee.csv, attendance.csv, leaves.csv, users.csv
├── docs/                         # CRC/method and data dictionaries
└── payrollhub/
    ├── Main.java                 # Swing application entry point
    ├── contracts/                # Role and service interfaces
    ├── data/                     # CsvHandler and GUI/backend record objects
    ├── gui/                      # Login, dashboard, and feature screens
    ├── model/                    # Employee hierarchy and domain models
    ├── service/                  # Authentication, attendance, leave, payroll services
    └── test/                     # Dependency-free test/integration entry points
```

## Data storage

All application records are stored in CSV files under `src/data/`, relative to the project root. `CsvHandler` centralizes CSV access; screens do not parse files directly. The checked-in files are `employee.csv`, `attendance.csv`, `leaves.csv`, and `users.csv`. `tickets.csv` is created when the first ticket is saved. There is no remote API or database connection.

See the [data dictionary](./src/docs/data-dictionary.md) for field formats and constraints.


## Login credentials

Login credentials that was pulled from 'users.csv' from each role.
Password is: Motorph123

| Role in `users.csv` | username |
|---|---|
| `Admin` | 'biancaa' or 'christianm' |
| `HumanResources` | 'rosiea' or 'andreav' |
| `Accounting` | 'leilam' or 'anthonys' |
| `Regular` | 'lizethv' or 'vellas' |
| `InfoTech` | 'eduardh' |

## Design decisions and limitations

- **CSV storage:** CSV is used to align with the project dataset and avoid a separate database setup.
- **Separated layers:** GUI components call the CSV data layer and payroll/attendance services; payroll calculations are not embedded in screen rendering.
- **Role checks:** Navigation and UI actions use centralized role checks in `NavAccess`.
- **Payroll validation:** Existing payroll methods and current payslip output need reconciliation with the authoritative MotorPH/course policy. Existing payroll tests currently report failures; confirm the rules before relying on figures.
- **Credential storage:** `users.csv` currently stores passwords as plaintext. This is a known security limitation; do not use real credentials.

## Academic notice

This project was created for MO-IT110 Object Oriented Programming at Mapúa-Malayan Digital College. The design templates remain the property of the college and are used for educational purposes only.
