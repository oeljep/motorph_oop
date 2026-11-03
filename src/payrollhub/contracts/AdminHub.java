package payrollhub.contracts;

import java.io.IOException;
import payrollhub.model.Employee;

public interface AdminHub {

    void addEmployee(Employee emp) throws IOException;

    int generateNextEmpNo();

    boolean isEmployeeValid(Employee emp);

    void updateEmployee(int id, Employee data) throws IOException;

    void removeEmployee(int id) throws IOException;
}
