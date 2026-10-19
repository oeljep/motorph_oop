package payrollhub.data;

import java.time.LocalDate;

/**
 * Carries one row of employee.csv (17 columns) between the file layer and
 * the GUI. It holds data only: no file access and no payroll rules.
 *
 * Column order (must match employee.csv):
 * 0 empNo, 1 lastName, 2 firstName, 3 birthday, 4 address, 5 phone, 6 sss,
 * 7 philhealth, 8 tin, 9 pagibig, 10 status, 11 position, 12 supervisor,
 * 13 basicSalary, 14 riceSubsidy, 15 phoneAllowance, 16 clothingAllowance
 *
 * Government IDs and phone numbers are kept as digits-only text (dashes are
 * stripped, e.g. "49-2959312-6" becomes "4929593126"). Derived values such as
 * the semi-monthly and hourly rate are NOT stored; PayrollCalculator computes
 * them from basicSalary.
 */

public class EmployeeData {

    public static final int FIELD_COUNT = 17;

    private String empNo;
    private String lastName;
    private String firstName;
    private LocalDate birthday;
    private String address;
    private String phone;
    private String sss;
    private String philhealth;
    private String tin;
    private String pagibig;
    private String status;
    private String position;
    private String supervisor;
    private double basicSalary;
    private double riceSubsidy;
    private double phoneAllowance;
    private double clothingAllowance;

    public EmployeeData(String empNo, String lastName, String firstName,
            LocalDate birthday, String address, String phone,
            String sss, String philhealth, String tin, String pagibig,
            String status, String position, String supervisor,
            double basicSalary, double riceSubsidy,
            double phoneAllowance, double clothingAllowance) {
        setEmpNo(empNo);
        setLastName(lastName);
        setFirstName(firstName);
        setBirthday(birthday);
        setAddress(address);
        setPhone(phone);
        setSss(sss);
        setPhilhealth(philhealth);
        setTin(tin);
        setPagibig(pagibig);
        setStatus(status);
        setPosition(position);
        setSupervisor(supervisor);
        setBasicSalary(basicSalary);
        setRiceSubsidy(riceSubsidy);
        setPhoneAllowance(phoneAllowance);
        setClothingAllowance(clothingAllowance);
    }

    // ---------------------- CSV conversion ----------------------

    /**
     * Builds an object from one parsed CSV row. Throws IllegalArgumentException if
     * it is malformed.
     */

    public static EmployeeData fromCsvRow(String[] row) {
        if (row.length != FIELD_COUNT) {
            throw new IllegalArgumentException("Expected " + FIELD_COUNT
                    + " fields, got " + row.length);
        }
        return new EmployeeData(
                row[0], row[1], row[2],
                LocalDate.parse(row[3]), row[4], row[5],
                row[6], row[7], row[8], row[9],
                row[10], row[11], row[12],
                Double.parseDouble(row[13]),
                Double.parseDouble(row[14]),
                Double.parseDouble(row[15]),
                Double.parseDouble(row[16]));
    }

    /**
     * Converts this object to a CSV row. The order of fields must match the
     * employee.csv header.
     */
    public String[] toCsvRow() {
        return new String[]{
                empNo, lastName, firstName,
                birthday.toString(), address, phone,
                sss, philhealth, tin, pagibig,
                status, position, supervisor,
                Double.toString(basicSalary),
                Double.toString(riceSubsidy),
                Double.toString(phoneAllowance),
                Double.toString(clothingAllowance)
        };
    }

    // ---------------------- Getters ----------------------
    public String getEmpNo()            { return empNo;}
    public String getLastName()         { return lastName;}
    public String getFirstName()        { return firstName;}
    public LocalDate getBirthday()      { return birthday;}
    public String getAddress()          { return address;}
    public String getPhone()            { return phone;}
    public String getSss()              { return sss;}
    public String getPhilhealth()       { return philhealth;}
    public String getTin()              { return tin;}
    public String getPagibig()          { return pagibig;}
    public String getStatus()           { return status;}
    public String getPosition()         { return position;}
    public String getSupervisor()       { return supervisor;}
    public double getBasicSalary()      { return basicSalary;}
    public double getRiceSubsidy()      { return riceSubsidy;}
    public double getPhoneAllowance()   { return phoneAllowance;}
    public double getClothingAllowance(){ return clothingAllowance;}
    public String getFullName()         { return firstName + " " + lastName; }

    // ---------------------- Setters ----------------------
    public void setEmpNo(String empNo)                 { this.empNo = requireText(empNo, "Employee No.");}
    public void setLastName(String lastName)           { this.lastName = requireText(lastName, "Last Name");}
    public void setFirstName(String firstName)         { this.firstName = requireText(firstName, "First Name");}
    public void setPosition(String position)            { this.position = requireText(position, "Position");}
    public void setStatus(String status)                { this.status = requireText(status, "Status");}
    public void setAddress(String address)             { this.address = requireDigits(address, "Address");}
    public void setPhone(String phone)                 { this.phone = requireDigits(phone, "Phone Number");}
    public void setSss(String sss)                      { this.sss = requireDigits(sss, "SSS");}
    public void setPhilhealth(String philhealth)        { this.philhealth = requireDigits(philhealth, "PhilHealth");}
    public void setTin(String tin)                      { this.tin = requireDigits(tin, "TIN");}
    public void setPagibig(String pagibig)              { this.pagibig = requireDigits(pagibig, "Pag-ibig");}
    
    public void setBasicSalary(double basicSalary)      { this.basicSalary = requirePositive(basicSalary, "Basic Salary");}
    public void setRiceSubsidy(double riceSubsidy)      { this.riceSubsidy = requirePositive(riceSubsidy, "Rice Subsidy");}
    public void setPhoneAllowance(double phoneAllowance) { this.phoneAllowance = requirePositive(phoneAllowance, "Phone Allowance");}
    public void setClothingAllowance(double clothingAllowance){ this.clothingAllowance = requirePositive(clothingAllowance, "Clothing Allowance");}
    
    public void setBirthday(LocalDate birthday) { 
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday is required");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birthday cannot be in the future");
        }
        this.birthday = birthday;
    }

        public void setSupervisor(String supervisor) { 
            this.supervisor = supervisor == null ? "" : supervisor.trim();
        }

    // ---------------------- Validation helpers ----------------------
    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String requireDigits(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String digitsOnly = value.replaceAll("\\D", "");
        if (digitsOnly.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must contain digits");
        }
        return digitsOnly;
    }

    private double requirePositive(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    /** Accepts "90000", "90000.5" and, defensively, "90,000". */
    private static double parseMoney(String text) {
        String cleaned = text.replace(",", "").trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("missing amount");
        }
        return Double.parseDouble(cleaned);
    }
 
    /** Writes 90000.0 as "90000" and 1234.5 as "1234.5". */
    private static String formatMoney(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }
 
    @Override
    public String toString() {
        return empNo + " - " + lastName + ", " + firstName + " (" + position + ")";
    }

}