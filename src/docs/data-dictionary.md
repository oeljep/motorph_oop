# Payroll Hub: Data Dictionary

MO-IT110 Milestone 1 · Section S2101 · Group 7

Reference for the CSV files in `src/data/`. All reading and writing goes through `CsvHandler`. If a column changes, update this file, `EmployeeData`, and `CsvHandler` together.

## Global conventions

| Topic | Rule |
|---|---|
| Encoding | UTF-8, one header row, no blank rows, no merged cells |
| Delimiter | Comma. Any field containing a comma (for example Address) is wrapped in double quotes |
| Empty values | Left blank (`,,`). Never `N/A`, `NULL` or `None`. Parse with `split(",", -1)` or a quote-aware parser so blank fields are kept |
| Identifiers | `empNo` is the employee key shared by the files |
| Government IDs and phone | Digits only, stored as text. Dashes are added for display only (helper in `GUIUtility`) |
| Dates | ISO `yyyy-MM-dd`, parsed to `java.time.LocalDate` |
| Times | 24-hour `H:mm`, parsed to `java.time.LocalTime` |
| Money | Plain number, no thousands separators, no currency symbol (for example `90000`, not `"90,000"`) |
| Derived values | Never stored. Semi-monthly rate, hourly rate, hours worked, late minutes, overtime, deductions, and net pay are computed in code |
| Names | Stored only in `employee.csv`. Other files reference `empNo` |

## employee.csv (17 columns)

| # | Column | Type | Required | Notes |
|---|---|---|---|---|
| 1 | empNo | int | Yes | Unique key |
| 2 | lastName | String | Yes | |
| 3 | firstName | String | Yes | |
| 4 | birthday | LocalDate | Yes | `yyyy-MM-dd`; must not be in the future |
| 5 | address | String | Yes | Quoted because it contains commas |
| 6 | phone | String | Yes | Digits only |
| 7 | sss | String | Yes | Digits only |
| 8 | philhealth | String | Yes | Digits only |
| 9 | tin | String | Yes | Digits only |
| 10 | pagibig | String | Yes | Digits only |
| 11 | status | String | Yes | `Regular` or `Probationary` |
| 12 | position | String | Yes | Matches the `Position` class |
| 13 | supervisor | String | No | Blank for the CEO (top-level). Skip supervisor validation when blank. GUI shows "None (top-level)" |
| 14 | basicSalary | double | Yes | Monthly, no commas |
| 15 | riceSubsidy | double | Yes | Monthly |
| 16 | phoneAllowance | double | Yes | Monthly |
| 17 | clothingAllowance | double | Yes | Monthly |

Removed from the source sheet (computed in `PayrollCalculator` or as `Employee` getters):

- Gross semi-monthly rate = basic salary ÷ 2 (verify against source rows)
- Hourly rate = basic salary ÷ 21 ÷ 8 (verify against source rows)

## attendance.csv (4 columns)

| # | Column | Type | Required | Notes |
|---|---|---|---|---|
| 1 | empNo | int | Yes | Must exist in `employee.csv` |
| 2 | date | LocalDate | Yes | `yyyy-MM-dd` |
| 3 | logIn | LocalTime | Yes | `H:mm` |
| 4 | logOut | LocalTime | No | `H:mm`. Blank until the employee times out |

Names were removed (duplicate of `employee.csv`). Hours worked, late minutes, and overtime are computed from logIn and logOut.

## leaves.csv (7 columns)

| # | Column | Type | Required | Notes |
|---|---|---|---|---|
| 1 | leaveID | String | Yes | Unique key |
| 2 | employeeID | int | Yes | Must exist in `employee.csv` |
| 3 | type | String | Yes | `Sick`, `Vacation`, or `Personal` |
| 4 | startDate | LocalDate | Yes | `yyyy-MM-dd` |
| 5 | endDate | LocalDate | Yes | Must not be before startDate |
| 6 | status | String | Yes | `PENDING`, `APPROVED`, or `REJECTED`. New requests always start as `PENDING` |
| 7 | reason | String | No | Quote if it contains commas |

## users.csv (4 columns)

| # | Column | Type | Required | Notes |
|---|---|---|---|---|
| 1 | empNo | int | Yes | Must exist in `employee.csv` |
| 2 | userName | String | Yes | Unique |
| 3 | password | String | Yes | Storage method is just plaintext. Never displayed on the recovery screen |
| 4 | role | String | Yes | `Admin`, `HumanResources`, `Accounting`, `InfoTech`, or `Regular`. Decides which dashboard opens |

## Integrity checks (run after every cleanup)

1. Every `empNo` in attendance, leaves, and users exists in `employee.csv`.
2. Every row has the expected number of fields (17 / 4 / 7 / 4).
3. No dates or times fail to parse.
4. Every supervisor value is blank or matches a real employee name.
5. Government ID lengths match the expected digit counts.
