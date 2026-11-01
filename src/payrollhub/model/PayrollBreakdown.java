package payrollhub.model;

public class PayrollBreakdown {

    private int empNo;
    private int month;
    private int year;
    private double gross;
    private double sss;
    private double philhealth;
    private double pagibig;
    private double withdholdingTax;
    private double totalDeductions;
    private double netPay;

    public PayrollBreakdown(int empNo, int month, int year, double gross, double sss, double philhealth,
            double pagibig) {
        this.empNo = empNo;
        this.month = month;
        this.year = year;
        this.gross = gross;
        this.sss = sss;
        this.philhealth = philhealth;
        this.pagibig = pagibig;
        this.totalDeductions = sss + philhealth + pagibig;
        this.netPay = gross - totalDeductions;
    }

    // Getters
    public int getEmpNo() {
        return empNo;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public double getGross() {
        return gross;
    }

    public double getSss() {
        return sss;
    }

    public double getPhilhealth() {
        return philhealth;
    }

    public double getPagibig() {
        return pagibig;
    }

    public double getWithdholdingTax() {
        return withdholdingTax;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

}
