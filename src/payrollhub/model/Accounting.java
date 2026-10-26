package payrollhub.model;

import java.io.File;
import payrollhub.contracts.FinanceHub;

public class Accounting implements FinanceHub {

    @Override
    public void batchProcessPayroll(String payPeriod) {
        // TODO: implement batch payroll processing
    }

    @Override
    public Payslip generatePayslip(int empNo) {
        return null;
    }

    @Override
    public Payslip generatePayslip(int empNo, String period) {
        return null;
    }

    @Override
    public File generateTaxReport(String quarter) {
        return null;
    }

    @Override
    public SummaryData generateDeductionSummary(String month) {
        return null;
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
