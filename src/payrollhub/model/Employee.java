package payrollhub.model;

import java.time.LocalDate;

public abstract class Employee {

    // Private: identity
    private int empNo;
    private String lastName;
    private String firstName;
    private String address;
    private String phone;
    private LocalDate birthday;

    // Private: Work-related information
    private String position;
    private String supervisor;
    private String status;

    // Private: government IDs
    private String sss;
    private String philhealth;
    private String tin;
    private String pagibig;

    // Protected: pay fields
    protected double basicSalary;
    protected double riceSubsidy;
    protected double phoneAllowance;
    protected double clothingAllowance;

    protected Employee(int empNo, String lastName, String firstName,
            String address, String phone, LocalDate birthday,
            String supervisor, String status, String position,
            String sss, String philhealth, String tin, String pagibig,
            double basicSalary, double riceSubsidy,
            double phoneAllowance, double clothingAllowance) {
        setEmpNo(empNo);
        setLastName(lastName);
        setFirstName(firstName);
        setAddress(address);
        setPhone(phone);
        setBirthday(birthday);
        setSupervisor(supervisor);
        setStatus(status);
        setSss(sss);
        setPhilhealth(philhealth);
        setTin(tin);
        setPagibig(pagibig);
        setPosition(position);
        setBasicSalary(basicSalary);
        setRiceSubsidy(riceSubsidy);
        setPhoneAllowance(phoneAllowance);
        setClothingAllowance(clothingAllowance);
    }

    // Getters
    public int getEmpNo() {
        return empNo;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    public String getPosition() {
        return position;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getSss() {
        return sss;
    }

    public String getPhilhealth() {
        return philhealth;
    }

    public String getTin() {
        return tin;
    }

    public String getPagibig() {
        return pagibig;
    }

    // Validating setters
    public void setEmpNo(int v) {
        if (v <= 0) {
            throw new IllegalArgumentException("Employee number must is required.");
        }
        this.empNo = v;
    }

    public void setBasicSalary(double v) {
        if (v < 0) {
            throw new IllegalArgumentException("Basic salary cannot be negative.");
        }
        this.basicSalary = v;
    }

    public void setRiceSubsidy(double v) {
        if (v < 0) {
            throw new IllegalArgumentException("Rice subsidy cannot be negative.");
        }
        this.riceSubsidy = v;
    }

    public void setPhoneAllowance(double v) {
        if (v < 0) {
            throw new IllegalArgumentException("Phone allowance cannot be negative.");
        }
        this.phoneAllowance = v;
    }

    public void setClothingAllowance(double v) {
        if (v < 0) {
            throw new IllegalArgumentException("Clothing allowance cannot be negative.");
        }
        this.clothingAllowance = v;
    }

    public void setLastName(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Last name is required.");
        }
        this.lastName = v;
    }

    public void setFirstName(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("First name is required.");
        }
        this.firstName = v;
    }

    public void setPosition(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Position is required.");
        }
        this.position = v;
    }

    public void setStatus(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Status is required.");
        }
        this.status = v;
    }

    public void setAddress(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Address is required.");
        }
        this.address = v;
    }

    public void setPhone(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Phone number is required.");
        }
        this.phone = v;
    }

    public void setSss(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("SSS is required.");
        }
        this.sss = v;
    }

    public void setPhilhealth(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("PhilHealth is required.");
        }
        this.philhealth = v;
    }

    public void setTin(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("TIN is required.");
        }
        this.tin = v;
    }

    public void setPagibig(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Pag-IBIG is required.");
        }
        this.pagibig = v;
    }

    public void setBirthday(LocalDate v) {
        if (v == null) {
            throw new IllegalArgumentException("Birthday is required.");
        }
        this.birthday = v;
    }

    // If supervisor is null or empty, throw an exception. Otherwise, the employee
    // is the CEO.
    public void setSupervisor(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Supervisor is required.");
        } else {
            position = "CEO";
        }
        this.supervisor = v;
    }

    // Abstract methods for payroll computation, to be implemented by a utility
    // class PayrollBreakdown
    public abstract double calculatePay();

    public abstract double getSssDeduction(double gross);

    public abstract double getPagibigDeduction(double gross);

    public abstract double getWithholdingTax(double gross);

    public abstract double getPhilHealthDeduction(double gross);

    // -------- Method helpers for sublcasses to implement abstract methods --------

    /**
     * Logs out the current user and resets the security context for the
     * application.
     * This method should be called when the user logs out of the application.
     */
    public abstract void logout(String token);

    /**
     * Validates the provided password against the employee's stored password.
     * This method should be implemented to check if the given password matches
     * the employee's credentials.
     *
     * @param password The password to validate.
     * @return true if the password is valid, false otherwise.
     */
    public abstract boolean isPasswordValid(String password);

    /** Abstract Override: Defines the mechanism for updating user credentials. */
    public abstract void resetPassword(String newPassword);

    /**
     * Abstract Override: Scores the complexity of the provided credential.
     * Encapsulation: Returns 0 (weak) to 5 (strong) based on character variety.
     */
    public abstract int getPasswordStrength(String password);

    /**
     * Abstract Override: Mandates role identification for UI rendering.
     */
    public abstract Position getRole();

    /** Formats gross and net pay for UI display. */
    public String viewSalary() {
        return String.format("Gross: %.2f, Net: %.2f", calculatePay(), calculatePay()
                - getSssDeduction(calculatePay()) - getPagibigDeduction(calculatePay())
                - getWithholdingTax(calculatePay()) - getPhilHealthDeduction(calculatePay()));
    }

    /** Formats personal information for UI display. */
    public String viewPersonalInformation() {
        return String.format("Name: %s, Position: %s, Status: %s, Supervisor: %s, Birthday: %s, Address: %s, Phone: %s",
                getFullName(), getPosition(), getStatus(), getSupervisor(), getBirthday(), getAddress(), getPhone());
    }

    /**
     * Checks a username and password pair against users.csv. If they match, the
     * controller opens the dashboard for that employee’s role.
     * Overloading (1): declared in UserHub. Blank values are rejected. A wrong
     * username
     * and a wrong password give the same failure result.
     */
    public abstract boolean authenticate(String username, String password);

    /**
     * Overloading (2): same name as (1) with a different parameter list
     * (one String instead of two). Null, blank or expired tokens return false.
     */
    public abstract boolean authenticate(String token);

    /**
     * Initiates the leave request process.
     */
    public abstract void applyLeave(String leaveID, int empNo, LocalDate startDate,
            LocalDate endDate, String reason);

    /**
     * Aggregates attendance logs.
     */
    public abstract double calculateTotalHoursWorked();

    /**
     * Derives the hourly rate from the monthly basic salary for payroll
     * computation.
     */
    public double getHourlyRate() {
        return basicSalary / 168; // 21 working days * 8 hours per day

    }
}
