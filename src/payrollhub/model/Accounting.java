package payrollhub.model;

import java.io.File;
import payrollhub.contracts.FinanceHub;
import payrollhub.contracts.UserHub;

public class Accounting implements FinanceHub, UserHub {

    @Override
    public void batchProcessPayroll(String payPeriod) {
        // TODO: implement batch payroll processing
    }

    @Override
    public void updateSalaryRates(int empNo, double newBasicSalary) {
        // TODO: implement salary rate update
    }

    @Override
    public Position getRole() {
        return Position.ACCOUNTING;
    }
}
