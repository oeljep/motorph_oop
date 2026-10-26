package payrollhub.contracts;

public interface PayrollProcessor {

    double calculatePay();

    double getSssDeduction(double gross);

    double getPagibigDeduction(double gross);

    double getWithholdingTax(double gross);

    double getPhilHealthDeduction(double gross);
}
