package payrollhub.contracts;

import payrollhub.model.Employee;

public interface AdminHub {

    void addEmployee(Employee emp);

    int generateNextEmpNo();

    boolean isEmployeeValid(Employee emp);

    void updateEmployee(int id, Employee data);

    void removeEmployee(int id);
}
