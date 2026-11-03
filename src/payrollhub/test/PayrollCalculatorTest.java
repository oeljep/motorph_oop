package payrollhub.test;

import payrollhub.model.Employee;
import payrollhub.service.PayrollCalculator;

/**
 * Dependency-free tests for PayrollCalculator (no JUnit needed).
 *
 * Compile and run from the project root:
 * javac -d bin $(find src test -name "*.java")
 * java -cp bin payrollhub.service.PayrollCalculatorTest
 *
 * Exit code is 0 when everything passes and 1 when anything fails.
 *
 * ONE THING TO CONNECT: the tests that need an Employee object call
 * employee(...) in the ADAPTER section below. Fill that in with your own
 * Regular (or other Employee subclass) and those tests will run. Until then
 * they are reported as SKIP, not FAIL. All other tests run as-is.
 *
 * EXPECTED VALUES are written by hand from the statutory tables, not
 * computed by the code under test, so a wrong rule shows up as a FAIL.
 * The rules these tests expect (each method takes gross pay):
 *
 * SSS below 3,250 -> 135.00; +22.50 for every 500 above; 24,750 and up ->
 * 1,125.00
 * PhilHealth 3% premium, employee pays half; premium floor 300, cap 1,800
 * (so the employee share runs 150.00 to 900.00)
 * Pag-IBIG 1,000-1,500 -> 1%; above 1,500 -> 2%; employee share capped
 * (PAGIBIG_CAP)
 * Tax 2023-onward TRAIN monthly table, applied to taxable income:
 * up to 20,833 -> 0
 * 20,833 - 33,333 -> 15% of the excess over 20,833
 * 33,333 - 66,667 -> 1,875.00 + 20% of the excess over 33,333
 * 66,667 - 166,667 -> 8,541.80 + 25% of the excess over 66,667
 * 166,667 - 666,667 -> 33,541.80 + 30% of the excess over 166,667
 * 666,667 and above -> 183,541.80 + 35% of the excess over 666,667
 *
 * If your team's sheet uses a different rule, change the expected value in
 * the test (or the constant below), not the calculator.
 */
public class PayrollCalculatorTest {

    /**
     * Pag-IBIG employee share cap. 100.00 follows the MotorPH sheet (max fund
     * salary 5,000). Pag-IBIG raised the max fund salary to 10,000 in 2024,
     * which makes the cap 200.00. Set this to whichever rule your team uses.
     */
    private static final double PAGIBIG_CAP = 100.00;

    // =====================================================================
    // ADAPTER: build an Employee for the tests that need one.
    // =====================================================================

    private static Employee employee(double basic, double rice, double phone, double clothing) {
        // Example (change to match your constructor):
        // return new Regular("10001", "Cruz", "Ana", ..., basic, rice, phone,
        // clothing);
        throw new UnsupportedOperationException("adapter not connected: employee()");
    }

    // =====================================================================
    // Tiny test harness
    // =====================================================================

    private interface TestBody {
        void run() throws Exception;
    }

    private static final double TOLERANCE = 0.01; // one centavo

    private static int passed = 0;
    private static int failed = 0;
    private static int skipped = 0;

    private static final PayrollCalculator calc = new PayrollCalculator();

    private static void test(String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (UnsupportedOperationException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("adapter not connected")) {
                skipped++;
                System.out.println("  SKIP  " + name + "  (" + e.getMessage() + ")");
            } else {
                failed++;
                System.out.println("  FAIL  " + name + "\n          -> " + e);
            }
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAIL  " + name + "\n          -> " + t);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static void assertMoney(double expected, double actual) {
        if (Math.abs(expected - actual) > TOLERANCE) {
            throw new AssertionError(String.format("expected <%.2f> but was <%.2f>", expected, actual));
        }
    }

    private static void assertThrowsIAE(TestBody body) {
        try {
            body.run();
        } catch (IllegalArgumentException expected) {
            return;
        } catch (UnsupportedOperationException e) {
            throw e; // lets the adapter SKIP logic see it
        } catch (Throwable t) {
            throw new AssertionError("expected IllegalArgumentException but got " + t);
        }
        throw new AssertionError("expected IllegalArgumentException but nothing was thrown");
    }

    // =====================================================================
    // Main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("\nfinalSSS");
        sssTests();
        System.out.println("\nfinalPhilHealth");
        philhealthTests();
        System.out.println("\nfinalPagIbig");
        pagibigTests();
        System.out.println("\nfinalWithholdingTax");
        taxTests();
        System.out.println("\npayAfterDeductions");
        payAfterDeductionsTests();
        System.out.println("\nSweeps across many salaries");
        sweepTests();
        System.out.println("\nEmployee-based methods (need the adapter)");
        employeeTests();

        System.out.println("\n----------------------------------------");
        System.out.println("Passed: " + passed + "   Failed: " + failed + "   Skipped: " + skipped);
        System.exit(failed == 0 ? 0 : 1);
    }

    // =====================================================================
    // SSS
    // =====================================================================

    private static void sssTests() {
        test("below the first bracket pays the minimum (135.00)", () -> {
            assertMoney(135.00, calc.finalSSS(0));
            assertMoney(135.00, calc.finalSSS(1000));
            assertMoney(135.00, calc.finalSSS(3249));
        });

        test("3,250 starts the next bracket (157.50)", () -> assertMoney(157.50, calc.finalSSS(3250)));

        test("brackets change at 3,750 (157.50 -> 180.00)", () -> {
            assertMoney(157.50, calc.finalSSS(3749));
            assertMoney(180.00, calc.finalSSS(3750));
        });

        test("mid-table: 10,000 falls in the 9,750-10,250 bracket (450.00)",
                () -> assertMoney(450.00, calc.finalSSS(10000)));

        test("last step before the cap is 1,102.50 (at 24,749)", () -> assertMoney(1102.50, calc.finalSSS(24749)));

        test("cap starts at 24,750 (1,125.00)", () -> assertMoney(1125.00, calc.finalSSS(24750)));

        test("high pay stays at the cap", () -> {
            assertMoney(1125.00, calc.finalSSS(32500));
            assertMoney(1125.00, calc.finalSSS(94500));
            assertMoney(1125.00, calc.finalSSS(1_000_000));
        });

        test("pay with centavos just under a bracket edge stays in the lower bracket", () -> {
            // The table says "below 3,250", so 3,249.99 pays 135.00.
            assertMoney(135.00, calc.finalSSS(3249.99));
            assertMoney(1102.50, calc.finalSSS(24749.99));
        });
    }

    // =====================================================================
    // PhilHealth (employee share)
    // =====================================================================

    private static void philhealthTests() {
        test("10,000 pays the floor share (150.00)", () -> assertMoney(150.00, calc.finalPhilHealth(10000)));

        test("middle pay is 1.5% of gross (30,000 -> 450.00)", () -> assertMoney(450.00, calc.finalPhilHealth(30000)));

        test("45,000 -> 675.00", () -> assertMoney(675.00, calc.finalPhilHealth(45000)));

        test("59,000 -> 885.00", () -> assertMoney(885.00, calc.finalPhilHealth(59000)));

        test("cap starts at 60,000 (900.00)", () -> assertMoney(900.00, calc.finalPhilHealth(60000)));

        test("high pay stays at the cap", () -> {
            assertMoney(900.00, calc.finalPhilHealth(94500));
            assertMoney(900.00, calc.finalPhilHealth(500_000));
        });

        test("pay below 10,000 still pays the floor share (5,000 -> 150.00)",
                () -> assertMoney(150.00, calc.finalPhilHealth(5000)));

        test("just under 60,000 is still 1.5% (59,999 -> 899.99)",
                () -> assertMoney(899.985, calc.finalPhilHealth(59999)));
    }

    // =====================================================================
    // Pag-IBIG (employee share)
    // =====================================================================

    private static void pagibigTests() {
        test("below 1,000 pays nothing", () -> {
            assertMoney(0.00, calc.finalPagIbig(0));
            assertMoney(0.00, calc.finalPagIbig(999.99));
        });

        test("1,000 to 1,500 uses 1%", () -> {
            assertMoney(10.00, calc.finalPagIbig(1000));
            assertMoney(12.00, calc.finalPagIbig(1200));
            assertMoney(15.00, calc.finalPagIbig(1500));
        });

        test("above 1,500 uses 2%", () -> {
            assertMoney(30.00, calc.finalPagIbig(1500.01));
            assertMoney(60.00, calc.finalPagIbig(3000));
            assertMoney(80.00, calc.finalPagIbig(4000));
        });

        test("share is capped (PAGIBIG_CAP) for high pay", () -> {
            assertMoney(PAGIBIG_CAP, calc.finalPagIbig(30000));
            assertMoney(PAGIBIG_CAP, calc.finalPagIbig(94500));
        });
    }

    // =====================================================================
    // Withholding tax
    // =====================================================================

    private static void taxTests() {
        test("up to 20,832 pays no tax", () -> {
            assertMoney(0.00, calc.finalWithholdingTax(0));
            assertMoney(0.00, calc.finalWithholdingTax(10000));
            assertMoney(0.00, calc.finalWithholdingTax(20832));
        });

        test("negative income pays no tax", () -> assertMoney(0.00, calc.finalWithholdingTax(-500)));

        test("20,833 is the start of the 15% bracket (tax 0.00)",
                () -> assertMoney(0.00, calc.finalWithholdingTax(20833)));

        test("15% bracket: 25,000 -> 625.05", () -> assertMoney(625.05, calc.finalWithholdingTax(25000)));

        test("20% bracket: 40,000 -> 3,208.40", () -> assertMoney(3208.40, calc.finalWithholdingTax(40000)));

        test("25% bracket: 70,000 -> 9,375.05", () -> assertMoney(9375.05, calc.finalWithholdingTax(70000)));

        test("30% bracket: 200,000 -> 43,541.70", () -> assertMoney(43541.70, calc.finalWithholdingTax(200000)));

        test("35% bracket: 700,000 -> 195,208.35", () -> assertMoney(195208.35, calc.finalWithholdingTax(700000)));

        test("tax at the bracket edges matches the table", () -> {
            assertMoney(1875.00, calc.finalWithholdingTax(33333));
            assertMoney(8541.80, calc.finalWithholdingTax(66667));
            assertMoney(33541.80, calc.finalWithholdingTax(166667));
            assertMoney(183541.80, calc.finalWithholdingTax(666667));
        });
    }

    // =====================================================================
    // payAfterDeductions
    // =====================================================================

    private static void payAfterDeductionsTests() {
        test("subtracts deductions from gross", () -> assertMoney(40000.00, calc.payAfterDeductions(50000, 10000)));

        test("zero deductions returns gross", () -> assertMoney(50000.00, calc.payAfterDeductions(50000, 0)));

        test("deductions equal to gross give zero", () -> assertMoney(0.00, calc.payAfterDeductions(1000, 1000)));

        test("negative gross is rejected", () -> assertThrowsIAE(() -> calc.payAfterDeductions(-1, 0)));

        test("negative deductions are rejected", () -> assertThrowsIAE(() -> calc.payAfterDeductions(1000, -1)));
    }

    // =====================================================================
    // Sweeps: rules that must hold for any pay
    // =====================================================================

    private static void sweepTests() {
        test("SSS never decreases and stays within 135-1,125", () -> {
            double previous = 0;
            for (int g = 0; g <= 100_000; g++) {
                double v = calc.finalSSS(g);
                check(v >= previous, "SSS dropped at gross " + g);
                check(v >= 135.00 && v <= 1125.00, "SSS out of range at gross " + g + ": " + v);
                previous = v;
            }
        });

        test("PhilHealth never decreases and never exceeds 900", () -> {
            double previous = 0;
            for (int g = 0; g <= 150_000; g++) {
                double v = calc.finalPhilHealth(g);
                check(v >= previous - 0.0001, "PhilHealth dropped at gross " + g);
                check(v <= 900.00 + 0.0001, "PhilHealth above 900 at gross " + g + ": " + v);
                previous = v;
            }
        });

        test("Pag-IBIG never decreases as pay rises", () -> {
            double previous = 0;
            for (int g = 0; g <= 150_000; g++) {
                double v = calc.finalPagIbig(g);
                check(v >= previous - 0.0001, "Pag-IBIG dropped at gross " + g);
                previous = v;
            }
        });

        test("tax is never negative and never decreases as income rises", () -> {
            double previous = 0;
            for (int income = 0; income <= 1_000_000; income++) {
                double v = calc.finalWithholdingTax(income);
                check(v >= 0, "tax negative at income " + income);
                check(v >= previous - 0.0001,
                        "tax dropped from " + previous + " to " + v + " at income " + income);
                previous = v;
            }
        });
    }

    // =====================================================================
    // Employee-based methods
    // =====================================================================

    private static void employeeTests() {
        test("finalGross adds basic and the three allowances",
                () -> assertMoney(32500.00, calc.finalGross(employee(30000, 1500, 500, 500))));

        test("finalGross with no allowances is just the basic salary",
                () -> assertMoney(30000.00, calc.finalGross(employee(30000, 0, 0, 0))));

        test("finalGross rejects null", () -> assertThrowsIAE(() -> calc.finalGross(null)));

        test("sumDeductions rejects null", () -> {
            assertThrowsIAE(() -> calc.sumDeductions(null));
            assertThrowsIAE(() -> calc.sumDeductions(null, "sss"));
        });

        test("sumDeductions(type) rejects null, blank and unknown types", () -> {
            Employee e = employee(30000, 1500, 500, 500);
            assertThrowsIAE(() -> calc.sumDeductions(e, null));
            assertThrowsIAE(() -> calc.sumDeductions(e, ""));
            assertThrowsIAE(() -> calc.sumDeductions(e, "   "));
            assertThrowsIAE(() -> calc.sumDeductions(e, "bonus"));
        });

        test("sumDeductions(type) returns the SSS and PhilHealth for gross 32,500", () -> {
            Employee e = employee(30000, 1500, 500, 500);
            assertMoney(1125.00, calc.sumDeductions(e, "sss"));
            assertMoney(487.50, calc.sumDeductions(e, "philhealth"));
        });

        test("deduction type names ignore case, spaces, dashes and underscores", () -> {
            Employee e = employee(30000, 1500, 500, 500);
            double sss = calc.sumDeductions(e, "sss");
            assertMoney(sss, calc.sumDeductions(e, " SSS "));

            double pagibig = calc.sumDeductions(e, "pagibig");
            assertMoney(pagibig, calc.sumDeductions(e, "Pag-IBIG"));
            assertMoney(pagibig, calc.sumDeductions(e, "pag_ibig"));
            assertMoney(pagibig, calc.sumDeductions(e, "Pag IBIG"));

            double philhealth = calc.sumDeductions(e, "philhealth");
            assertMoney(philhealth, calc.sumDeductions(e, "Phil Health"));

            double tax = calc.sumDeductions(e, "tax");
            assertMoney(tax, calc.sumDeductions(e, "withholding"));
            assertMoney(tax, calc.sumDeductions(e, "Withholding Tax"));
        });

        test("total / all / deductions aliases return the full sum", () -> {
            Employee e = employee(30000, 1500, 500, 500);
            double total = calc.sumDeductions(e);
            assertMoney(total, calc.sumDeductions(e, "total"));
            assertMoney(total, calc.sumDeductions(e, "ALL"));
            assertMoney(total, calc.sumDeductions(e, "statutory"));
            assertMoney(total, calc.sumDeductions(e, "deductions"));
        });

        test("the full sum equals SSS + PhilHealth + Pag-IBIG + tax", () -> {
            Employee e = employee(30000, 1500, 500, 500);
            double parts = calc.sumDeductions(e, "sss")
                    + calc.sumDeductions(e, "philhealth")
                    + calc.sumDeductions(e, "pagibig")
                    + calc.sumDeductions(e, "tax");
            assertMoney(parts, calc.sumDeductions(e));
        });

        test("tax in sumDeductions is charged on gross minus SSS, PhilHealth and Pag-IBIG", () -> {
            Employee e = employee(30000, 1500, 500, 500);
            double gross = calc.finalGross(e);
            double taxable = gross
                    - calc.finalSSS(gross)
                    - calc.finalPhilHealth(gross)
                    - calc.finalPagIbig(gross);
            assertMoney(calc.finalWithholdingTax(taxable), calc.sumDeductions(e, "tax"));
        });
    }
}
