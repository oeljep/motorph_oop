# Payroll Hub

A Java desktop payroll system for **MotorPH**, built with object-oriented programming. Payroll Hub handles employee records, time tracking, leave filing and approval, statutory deductions, and payslip generation through a Swing GUI, with data stored in CSV files.

> **MO-IT110 Object-Oriented Programming · Milestone 1**
> Mapúa-Malayan Digital College · Section S2101 · Group 7

---

## Features

- **Role-based login.** One login screen for everyone; the dashboard that opens is derived from the authenticated user's role, not chosen by the user.
- **Employee management.** View, search, add, update and delete employee records (Admin / HR).
- **Timesheet.** Employees record time in and time out; late minutes, hours worked and overtime are computed from the attendance record.
- **Leave management.** Staff file leave requests; Admin and HR approve or reject them.
- **Payroll computation.** Gross pay, SSS, PhilHealth, Pag-IBIG, withholding tax (TRAIN Law) and net pay.
- **Payslips.** Generate a payslip for a chosen cut-off period.
- **Credential reset.** IT staff handle password resets, and users get a recovery screen that never reveals the stored password.

## User roles

| Role | Class | What they can do |
|---|---|---|
| Admin | `Admin` | Add, update and remove employees; approve or reject leave |
| Human Resources | `HumanResources` | Update employee details; review and decide leave requests |
| Accounting | `Accounting` | Batch payroll, payslips, tax and deduction reports |
| IT | `InfoTech` | Reset credentials, check system health |
| Regular Staff | `Regular` | View own profile and salary, time in/out, file leave |

## OOP design

The design package (CRC cards and Method Dictionary) is organized by OOP pillar.

| Pillar | How it is applied |
|---|---|
| **Encapsulation** | Identity and credentials are `private`; salary fields are `protected`. Setters validate input. `PayrollBreakdown` is immutable (`private final`, no setters). `LeaveRequest` always starts as `PENDING`. |
| **Abstraction** | `Employee` is abstract. Interfaces define role contracts: `UserHub`, `AdminHub`, `HRHub`, `FinanceHub`, `SystemSupportHub`, `LeaveHub`, `PayrollProcessor`. |
| **Inheritance** | `Admin`, `HumanResources`, `Accounting`, `InfoTech` and `Regular` extend `Employee` and call `super(...)` to initialize shared identity and pay fields. |
| **Polymorphism** | Overriding: each role provides its own `CalculatePay()`. Overloading: `authenticate(user, pass)` / `authenticate(token)`, `generatePayslip(...)`, `resetCredentials(...)`, `sumDeductions(...)`. A generic `Employee` is cast to an interface (for example `LeaveHub`) to unlock role-specific actions. |

## Project structure

```
PayrollHub/
├── src/
│   └── payrollhub/
│       ├── PayrollSystem.java        # Controller: start, login, dashboard routing, logout, exit
│       ├── model/                    # Employee hierarchy and domain objects
│       │   ├── Employee.java
│       │   ├── Admin.java
│       │   ├── HumanResources.java
│       │   ├── Accounting.java
│       │   ├── InfoTech.java
│       │   ├── Regular.java
│       │   ├── Position.java
│       │   ├── LeaveRequest.java
│       │   ├── Attendance.java
│       │   ├── PayrollBreakdown.java
│       │   ├── PeriodSummary.java
│       │   └── ITTicket.java
│       ├── contracts/                # Interfaces
│       │   ├── UserHub.java
│       │   ├── AdminHub.java
│       │   ├── HRHub.java
│       │   ├── FinanceHub.java
│       │   ├── SystemSupportHub.java
│       │   ├── LeaveHub.java
│       │   └── PayrollProcessor.java
│       ├── service/
│       │   └── PayrollCalculator.java
│       ├── data/                     # File layer
│       │   ├── CsvHandler.java
│       │   └── EmployeeData.java
│       └── gui/                      # Swing screens
│           ├── GUILogin.java
│           ├── GUIForgotPass.java
│           ├── GUIDashboard.java
│           ├── GUITimesheet.java
│           ├── GUILeave.java
│           ├── GUIPayslip.java
│           ├── GUIAddEmployee.java
│           └── GUIUtility.java
├── data/                             # CSV "database"
│   ├── employees.csv
│   ├── attendance.csv
│   ├── leaves.csv
│   └── users.csv
├── docs/                             # Design package and process flowchart
└── README.md
```

## Data files

Employee and attendance data come from the MotorPH dataset. All reading and writing goes through `CsvHandler`, so no screen parses CSV on its own. `EmployeeData` carries one employee row (19 fields) between the file layer and the GUI.

## Getting started

### Prerequisites

- JDK 17 or later
- VS Code with the **Extension Pack for Java** (or any Java IDE)

### Run in VS Code

1. Clone the repository:
   ```bash
   git clone <your-repo-url>
   cd PayrollHub
   ```
2. Open the folder in VS Code.
3. Open `src/payrollhub/PayrollSystem.java` and click **Run** above the `main` method.

Run the app from the project root so the relative `data/` path resolves.

### Run from the command line

```bash
javac -d bin $(find src -name "*.java")
java -cp bin payrollhub.PayrollSystem
```

On Windows PowerShell:

```powershell
javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp bin payrollhub.PayrollSystem
```

## Design decisions

- **CSV over a database.** The source data are spreadsheets and CSV files, and a database would add setup without serving the OOP goals.
- **Interface checks over role flags.** Approve/decline and add/delete controls appear only when the user's class implements the matching interface (`instanceof`), so access is enforced by the compiler rather than an editable flag.
- **Payroll rules stay out of the GUI.** SSS, PhilHealth, Pag-IBIG and tax logic live in `PayrollCalculator`, so a UI change can never alter a computation.
- **Stateless utilities.** `CsvHandler` and `GUIUtility` use static methods and hold no state.

## Team

**Group 7, Section S2101**

- Rowel Jepsani
- Kristel Austria
- Russell Jae Udaundo
- Isabelle Angeli Gallardo

## Academic notice

This project was created for MO-IT110 at Mapúa-Malayan Digital College. The design templates remain the property of the college and are used for educational purposes only.
