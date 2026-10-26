package payrollhub.service;

import payrollhub.data.EmployeeData;

public class PayrollCalculator {

    public double calculateGrossPay(EmployeeData employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        return employee.getBasicSalary() + employee.getRiceSubsidy()
                + employee.getPhoneAllowance() + employee.getClothingAllowance();
    }
}
