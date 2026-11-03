package payrollhub.service;

import java.util.Locale;

import payrollhub.model.Employee;

public class PayrollCalculator {

    // Returns the gross pay by delegating to the employee’s own CalculatePay()
    public double finalGross(Employee empNo) {
        if (empNo == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        return empNo.getBasicSalary() + empNo.getRiceSubsidy()
                + empNo.getPhoneAllowance() + empNo.getClothingAllowance();
    }

    // Determines the SSS deduction using the 2024/2025 statutory table.
    public double finalSSS(double gross) {
        double[] limits = {
                3249, 3749, 4249, 4749, 5249, 5749, 6249, 6749, 7249, 7749,
                8249, 8749, 9249, 9749, 10249, 10749, 11249, 11749, 12249,
                12749, 13249, 13749, 14249, 14749, 15249, 15749, 16249, 16749,
                17249, 17749, 18249, 18749, 19249, 19749, 20249, 20749, 21249,
                21749, 22249, 22749, 23249, 23749, 24249, 24749
        };
        double[] contributions = {
                135, 157.5, 180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5,
                360, 382.5, 405, 427.5, 450, 472.5, 495, 517.5, 540, 562.5,
                585, 607.5, 630, 652.5, 675, 697.5, 720, 742.5, 765, 787.5,
                810, 832.5, 855, 877.5, 900, 922.5, 945, 967.5, 990, 1012.5,
                1035, 1057.5, 1080, 1102.5, 1125
        };
        for (int i = 0; i < limits.length; i++) {
            if (gross <= limits[i]) {
                return contributions[i];
            }
        }
        return 1125;
    }

    // Calculates the employee's half of the
    // 3% PhilHealth premium based on gross pay, capped at 1,800.
    public double finalPhilHealth(double gross) {
        double[] range = {
                10000, 59999.99
        };
        double[] rate = {
                0.03, 0.03
        };
        double[] premium = {
                300, 1799
        };
        for (int i = 0; i < range.length; i++) {
            if (gross <= range[i]) {
                return Math.min(gross * rate[i], premium[i]) * 0.5;
            }
        }
        return 1800 * 0.5;
    }

    // Calculates the Pag-IBIG contribution: 0 below 1,000, 1% up to 1,500,
    // and 2% for gross pay above 1,500.
    public double finalPagIbig(double gross) {
        if (gross < 1000) {
            return 0;
        }
        if (gross <= 1500) {
            return gross * 0.01;
        }
        return gross * 0.02;
    }

    // Computes withholding tax based on BIR/TRAIN law brackets after applying
    // deduction
    public double finalWithholdingTax(double gross) {
        if (gross <= 20832.00) {
            return 0.00;
        }
        // Bracket 2: Over P20,832 to P33,333 -> 15% in excess of P20,832
        else if (gross <= 33333.00) {
            return (gross - 20832.00) * 0.15;
        }
        // Bracket 3: Over P33,333 to P66,667 -> P1,875 + 20% in excess of P33,333
        else if (gross <= 66667.00) {
            return 1875.00 + ((gross - 33333.00) * 0.20);
        }
        // Bracket 4: Over P66,667 to P166,667 -> P8,541.80 + 25% in excess of P66,667
        else if (gross <= 166667.00) {
            return 8541.80 + ((gross - 66667.00) * 0.25);
        }
        // Bracket 5: Over P166,667 to P666,667 -> P33,541.80 + 30% in excess of
        // P166,667
        else if (gross <= 666670.00) {
            return 33541.80 + ((gross - 166667.00) * 0.30);
        }
        // Bracket 6: Over P666,667 -> P183,541.80 + 35% in excess of P666,667
        else {
            return 183541.80 + ((gross - 666670.00) * 0.35);
        }
    }

    // The Firewall logic: Subtracts all deductions from the gross salary.
    public double payAfterDeductions(double gross, double deduct) {
        if (gross < 0) {
            throw new IllegalArgumentException("Gross salary cannot be negative.");
        }
        if (deduct < 0) {
            throw new IllegalArgumentException("Deductions cannot be negative.");
        }
        return gross - deduct;
    }

    // Full Summary: Aggregates all government deductions.
    public double sumDeductions(Employee empNo) {
        if (empNo == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        double gross = finalGross(empNo);
        return finalSSS(gross)
                + finalPhilHealth(gross)
                + finalPagIbig(gross)
                + finalWithholdingTax(gross);
    }

    // Specific Summary: Targets one specific deduction type.
    public double sumDeductions(Employee empNo, String type) {
        if (empNo == null) {
            throw new IllegalArgumentException("Employee data is required.");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Deduction type is required.");
        }

        double gross = finalGross(empNo);
        String normalized = type.trim().toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");

        switch (normalized) {
            case "sss":
                return finalSSS(gross);
            case "philhealth":
                return finalPhilHealth(gross);
            case "pagibig":
                return finalPagIbig(gross);
            case "withholdingtax":
            case "tax":
            case "withholding":
                return finalWithholdingTax(gross);
            case "total":
            case "all":
            case "statutory":
            case "deduction":
            case "deductions":
                return sumDeductions(empNo);
            default:
                throw new IllegalArgumentException("Unsupported deduction type: " + type);
        }
    }
}
