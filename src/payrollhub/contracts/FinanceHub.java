package payrollhub.contracts;

import java.io.File;
import payrollhub.model.Position;
import payrollhub.model.Payslip;
import payrollhub.model.SummaryData;

public interface FinanceHub {

    Position getRole();

    void batchProcessPayroll(String payPeriod);

    Payslip generatePayslip(int empNo);

    Payslip generatePayslip(int empNo, String period);

    File generateTaxReport(String quarter);

    SummaryData generateDeductionSummary(String month);

    void updateSalaryRates(int empNo, double newBasicSalary);
}
