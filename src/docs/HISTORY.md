# Payroll Hub Project History

This document summarizes notable repository commits and records implementation
inconsistencies that were identified during project reviews. Commit references
and dates come from the repository history available when this document was
written (November 12, 2026). It is a milestone summary, not a complete
commit-by-commit changelog.

## Milestones

### October 7, 2026 — Project and domain foundation

- `9a112ad` established the initial MotorPH Payroll project structure.
- `b1d9c85` added the initial project README.
- `8b6c6bb` added role and service contracts, employee-role models, and early
  GUI components for employee, leave, payslip, and timesheet workflows.

### October 15–23, 2026 — CSV data layer and GUI restructuring

- `f22bf7b` completed the first CSV data layer for employee, attendance, leave,
  and user records, including handling a UTF-8 BOM in CSV input.
- `18c696c`, `ceeda5f`, `a5a86df`, and `137da3a` restructured files and the
  project classpath as the GUI and data code grew.
- `14005c5` updated the README, data dictionary, application logic, CSV
  handling, and employee data management.
- `8b3401e` expanded contracts, model behavior, dashboard, login, and CSV
  handling.

### October 27–31, 2026 — Role typing, leave, and payroll logic

- `d478f56` added leave request management GUI and service behavior.
- `d7a72ee` simplified the Accounting and Admin models.
- `e359f2d` changed role-model `getRole()` methods to return the `Position`
  enum rather than role-name strings.
- `c5f38f7` revised leave method contracts and added payroll deduction
  calculations.

### November 3–4, 2026 — Employee-facing workflows and leave controls

- `db0cbe2` added payroll and navigation tests, plus a larger set of GUI/data
  features including payslips, tickets, profiles, and attendance handling.
- `2da5574` added GUI components for payslip, ticket management, and employee
  profiles.
- `b6e8270` added role-specific management and personal dashboard summaries.
- `2caf945` implemented leave management and updated dashboard navigation.
- `5be9ba5` restricted leave decisions to direct reports and added integration
  coverage for the leave manager.
- `2ef33b0` updated leave sample data for testing.

### November 12, 2026 — Navigation and role reconciliation

- `8487b5b` refactored navigation access and related GUI components, including
  updates to `NavAccess`, sidebar screens, and the method dictionary.
- `4757a2d` updated README role/login information, adjusted navigation access,
  and moved the `NavAccessTest` source into `src/payrollhub/test`.
- Follow-up project work established the canonical role names as `Admin`,
  `HumanResources`, `Accounting`, `InfoTech`, and `Regular`, and reconciled the
  dashboard role-to-tab matrix and Admin-only leave decisions.

### November 13, 2026 — Automatic overtime in payroll

- Completed attendance now contributes overtime pay for paid hours above eight
  per day. Payroll applies the confirmed 1.25x hourly-rate multiplier without
  supervisor approval, and includes the amount in cutoff gross, deductions,
  payslip display, and payroll export.
- Added regression checks for overtime hours, pay, deductions, no-overtime
  shifts, and the payslip fields. The 1.25x rate remains a configured ordinary
  day assumption that should be verified against authoritative policy.

## Known issues and inconsistencies

These are items to verify or resolve; they are not claims that every item is
currently failing at runtime.

### Role and action permissions

- The role-to-tab matrix is centralized in `NavAccess`, but employee CRUD
  permissions are separate checks. In the current source,
  `canAddOrDeleteEmployee()` allows `HumanResources`, while the agreed policy
  was Admin for Employee Manager and Leave Manager, and HumanResources for
  Employee Manager only. Confirm the intended add, edit, and delete split and
  make the action checks match it; tab visibility alone does not enforce CRUD
  policy.
- Older interfaces and role-model methods still describe HR leave approval,
  even though the active GUI policy assigns leave decisions to Admin. Keep
  these legacy contracts clearly marked or remove/implement them as part of a
  planned model cleanup rather than treating them as active authorization.
- Earlier commits and documentation contained overlapping role names and
  access mechanisms. Keep `users.csv`, `NavAccess`, screen action checks,
  tests, and the method dictionary aligned when changing the role matrix.
- Employee Manager and the dashboard employee table are scoped to the signed-in
  Admin or HumanResources user's direct reports. Dashboard summary cards remain
  company-wide; do not mistake those aggregates for the employee list.

### Ticket and dispute identity

- The data layer persists support cases as `TicketData` records with `T` IDs.
  The application has a separate Dispute Manager tab, but historically the two
  manager screens did not have consistently distinct behavior or labels.
- The intended shared-case convention is `T0001` in Ticket Manager and
  `D0001` in Dispute Manager for the same underlying record. A D identifier is
  a display mapping, not a separately persisted dispute record. Keep the
  mapping stable and avoid creating a second ID sequence unless the data model
  is deliberately split.

### Tests and build configuration

- Test sources are stored under `src/payrollhub/test`; the `pom.xml` does not
  currently configure a JUnit dependency or a separate Maven test-source
  directory. The dependency-free test mains can be compiled and run directly,
  but `mvn test` is not a reliable validation path until the Maven test setup
  is configured.
- The payroll calculator test's deduction expectations now pass, but its
  employee adapter is still disconnected, leaving eight employee-based checks
  skipped. Payroll results should still be compared with the authoritative
  course/MotorPH rules before being treated as production-verified.
- A successful Java compile verifies source compatibility, not GUI behavior or
  every test assertion. Continue to run the focused navigation, data-layer,
  and GUI integration checks and report their results separately.

### Data and operational limitations

- Application records are CSV-backed and local to the project. Tickets are
  created in `tickets.csv` on first save; code and documentation should not
  imply that a database or remote service is available.
- `users.csv` stores plaintext passwords. This is a known limitation for an
  academic local application; real credentials must not be used.
- IT operations remains unavailable as a dashboard workflow. Overtime is
  calculated automatically from completed attendance, paid at the configured
  1.25x ordinary hourly rate, and does not require supervisor approval. Confirm
  the overtime multiplier against authoritative MotorPH/course policy,
  especially for rest days and holidays, before treating it as production-ready.

## Verification note

At the time of this document update, Java 17 compilation,
`NavAccessTest`, `GuiBackendIntegrationTest`, and `PayrollCalculatorTest`
passed. The payroll test reported 40 passed and eight skipped employee-adapter
checks. Maven was unavailable, so no Maven test run is claimed here.
Working-tree edits that have not been committed are not part of the commit
milestone list above.
