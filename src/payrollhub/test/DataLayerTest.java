package payrollhub.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import payrollhub.data.CsvHandler;
import payrollhub.data.EmployeeData;

import javax.swing.table.DefaultTableModel;

/**
 * Dependency-free tests for the data layer (CsvHandler and EmployeeData).
 * No JUnit needed: it compiles and runs with plain javac/java.
 *
 * File-based tests run against a temporary folder, so your real data/ files
 * are never touched (the only exception is the optional read-only check at
 * the bottom, which just loads data/employee.csv if it exists).
 *
 * Compile (from the project root):
 * javac -d bin $(find src test -name "*.java")
 * java -cp bin payrollhub.data.DataLayerTest
 *
 * PowerShell:
 * javac -d bin (Get-ChildItem -Recurse src,test -Filter *.java).FullName
 * java -cp bin payrollhub.data.DataLayerTest
 *
 * Exit code is 0 when everything passes and 1 when anything fails.
 */
public class DataLayerTest {

    // =====================================================================
    // Tiny test harness
    // =====================================================================

    private interface TestBody {
        void run() throws Exception;
    }

    private static int passed = 0;
    private static int failed = 0;
    private static Path tempDir;

    private static void test(String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAIL  " + name + "\n          -> " + t);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertEquals(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.0001) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertArrayEquals(String[] expected, String[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " but was " + Arrays.toString(actual));
        }
    }

    private static void assertThrows(Class<? extends Throwable> type, TestBody body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (type.isInstance(t))
                return;
            throw new AssertionError("expected " + type.getSimpleName()
                    + " but got " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    // =====================================================================
    // Sample data
    // =====================================================================

    /** A valid row in the exact 17-column order of employee.csv. */
    private static String[] validRow() {
        return new String[] {
                "10001", "Garcia", "Manuel III", "1983-10-11",
                "Valero Carpark Building Valero Street 1227, Makati City",
                "966-860-270", "44-4506057-3", "820126853846", "442-605-657-000", "691295330870",
                "Regular", "Chief Executive Officer", "",
                "90000", "1500", "2000", "1000"
        };
    }

    private static EmployeeData sample(String empNo, String last, String first, String supervisor) {
        return EmployeeData.fromCsvRow(new String[] {
            empNo, last, first, "1990-01-15", "Quezon City", "912345678",
            "4412345678", "820126853846", "442605657", "691295330870",
            "Regular", "Payroll Clerk", supervisor, "30000", "1500", "500", "500"
        });
    }

    private static final String[] HEADER = CsvHandler.EMPLOYEE_HEADER;

    private static Path newEmployeeFile(String name, EmployeeData... people) throws IOException {
        Path file = tempDir.resolve(name);
        List<String[]> rows = new ArrayList<>();
        for (EmployeeData p : people)
            rows.add(p.toCsvRow());
        CsvHandler.writeAll(file, HEADER, rows);
        return file;
    }

    // =====================================================================
    // Main
    // =====================================================================

    public static void main(String[] args) throws Exception {
        tempDir = Files.createTempDirectory("payrollhub-test");
        try {
            System.out.println("\nCsvHandler: parseLine / toCsvLine");
            parseLineTests();

            System.out.println("\nEmployeeData");
            employeeDataTests();

            System.out.println("\nCsvHandler: file operations");
            fileTests();

            System.out.println("\nCsvHandler: employee.csv operations");
            employeeFileTests();

            System.out.println("\nCsvHandler: supervisor / attendance / table");
            lookupTests();

            System.out.println("\nReal data (read-only)");
            realDataTests();
        } finally {
            deleteRecursively(tempDir);
        }

        System.out.println("\n----------------------------------------");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    // =====================================================================
    // CsvHandler: parseLine / toCsvLine
    // =====================================================================

    private static void parseLineTests() {
        test("parseLine splits simple fields",
                () -> assertArrayEquals(new String[] { "a", "b", "c" }, CsvHandler.parseLine("a,b,c")));

        test("parseLine trims whitespace around fields",
                () -> assertArrayEquals(new String[] { "a", "b", "c" }, CsvHandler.parseLine(" a , b ,c ")));

        test("parseLine keeps a comma inside quotes",
                () -> assertArrayEquals(new String[] { "Makati, Metro Manila", "x" },
                        CsvHandler.parseLine("\"Makati, Metro Manila\",x")));

        test("parseLine turns doubled quotes into one quote", () -> assertArrayEquals(new String[] { "say \"hi\"" },
                CsvHandler.parseLine("\"say \"\"hi\"\"\"")));

        test("parseLine keeps empty fields (blank supervisor)",
                () -> assertArrayEquals(new String[] { "a", "", "c" }, CsvHandler.parseLine("a,,c")));

        test("parseLine keeps a trailing empty field",
                () -> assertArrayEquals(new String[] { "a", "b", "" }, CsvHandler.parseLine("a,b,")));

        test("parseLine on an empty line gives one empty field",
                () -> assertArrayEquals(new String[] { "" }, CsvHandler.parseLine("")));

        test("toCsvLine quotes only fields that need it",
                () -> assertEquals("a,\"b,c\",d", CsvHandler.toCsvLine(new String[] { "a", "b,c", "d" })));

        test("toCsvLine escapes quotes and writes null as empty",
                () -> assertEquals("\"say \"\"hi\"\"\",", CsvHandler.toCsvLine(new String[] { "say \"hi\"", null })));

        test("toCsvLine then parseLine round-trips tricky values", () -> {
            String[] original = { "x", "has, comma", "has \"quote\"", "", "end" };
            assertArrayEquals(original, CsvHandler.parseLine(CsvHandler.toCsvLine(original)));
        });
    }

    // =====================================================================
    // EmployeeData
    // =====================================================================

    private static void employeeDataTests() {
        test("fromCsvRow reads a valid row", () -> {
            EmployeeData e = EmployeeData.fromCsvRow(validRow());
            assertEquals("10001", e.getEmpNo());
            assertEquals("Garcia", e.getLastName());
            assertEquals("Manuel III", e.getFirstName());
            assertEquals(LocalDate.of(1983, 10, 11), e.getBirthday());
            assertEquals("Regular", e.getStatus());
            assertEquals("Chief Executive Officer", e.getPosition());
            assertEquals(90000.0, e.getBasicSalary());
            assertEquals(1500.0, e.getRiceSubsidy());
            assertEquals(2000.0, e.getPhoneAllowance());
            assertEquals(1000.0, e.getClothingAllowance());
        });

        test("fromCsvRow keeps the comma inside the address",
                () -> assertEquals("Valero Carpark Building Valero Street 1227, Makati City",
                        EmployeeData.fromCsvRow(validRow()).getAddress()));

        test("government IDs and phone are stored as digits only", () -> {
            EmployeeData e = EmployeeData.fromCsvRow(validRow());
            assertEquals("966860270", e.getPhone());
            assertEquals("4445060573", e.getSss());
            assertEquals("442605657000", e.getTin());
        });

        test("blank supervisor is stored as empty string (the CEO)",
                () -> assertEquals("", EmployeeData.fromCsvRow(validRow()).getSupervisor()));

        test("fromCsvRow rejects too few fields", () -> assertThrows(IllegalArgumentException.class,
                () -> EmployeeData.fromCsvRow(new String[] { "10001", "Garcia" })));

        test("fromCsvRow rejects too many fields", () -> {
            String[] row = Arrays.copyOf(validRow(), 18);
            row[17] = "extra";
            assertThrows(IllegalArgumentException.class, () -> EmployeeData.fromCsvRow(row));
        });

        test("fromCsvRow rejects a bad birthday format", () -> {
            String[] row = validRow();
            row[3] = "10/11/1983";
            assertThrows(RuntimeException.class, () -> EmployeeData.fromCsvRow(row));
        });

        test("fromCsvRow rejects a non-numeric salary", () -> {
            String[] row = validRow();
            row[13] = "lots";
            assertThrows(RuntimeException.class, () -> EmployeeData.fromCsvRow(row));
        });

        test("fromCsvRow rejects a blank salary", () -> {
            String[] row = validRow();
            row[13] = "";
            assertThrows(IllegalArgumentException.class, () -> EmployeeData.fromCsvRow(row));
        });

        test("fromCsvRow accepts a salary with a thousands comma", () -> {
            String[] row = validRow();
            row[13] = "90,000";
            assertEquals(90000.0, EmployeeData.fromCsvRow(row).getBasicSalary());
        });

        test("setters reject blank required text", () -> {
            EmployeeData e = sample("1", "Cruz", "Ana", "");
            assertThrows(IllegalArgumentException.class, () -> e.setLastName("  "));
            assertThrows(IllegalArgumentException.class, () -> e.setFirstName(null));
            assertThrows(IllegalArgumentException.class, () -> e.setAddress(""));
            assertThrows(IllegalArgumentException.class, () -> e.setPosition(" "));
        });

        test("setBirthday rejects null and future dates", () -> {
            EmployeeData e = sample("1", "Cruz", "Ana", "");
            assertThrows(IllegalArgumentException.class, () -> e.setBirthday(null));
            assertThrows(IllegalArgumentException.class,
                    () -> e.setBirthday(LocalDate.now().plusDays(1)));
        });

        test("ID setters reject text with no digits", () -> {
            EmployeeData e = sample("1", "Cruz", "Ana", "");
            assertThrows(IllegalArgumentException.class, () -> e.setSss("abc"));
            assertThrows(IllegalArgumentException.class, () -> e.setTin("---"));
            assertThrows(IllegalArgumentException.class, () -> e.setPhone(null));
        });

        test("salary and allowance setters reject negatives and NaN", () -> {
            EmployeeData e = sample("1", "Cruz", "Ana", "");
            assertThrows(IllegalArgumentException.class, () -> e.setBasicSalary(-1));
            assertThrows(IllegalArgumentException.class, () -> e.setRiceSubsidy(-0.01));
            assertThrows(IllegalArgumentException.class, () -> e.setPhoneAllowance(Double.NaN));
            e.setClothingAllowance(0); // zero is allowed
            assertEquals(0.0, e.getClothingAllowance());
        });

        test("setSupervisor treats null as empty and trims", () -> {
            EmployeeData e = sample("1", "Cruz", "Ana", "Garcia, Manuel III");
            e.setSupervisor(null);
            assertEquals("", e.getSupervisor());
            e.setSupervisor("  Lim, Antonio ");
            assertEquals("Lim, Antonio", e.getSupervisor());
        });

        test("toCsvRow has 17 fields and formats whole money without .0", () -> {
            String[] row = sample("1", "Cruz", "Ana", "").toCsvRow();
            assertEquals(EmployeeData.FIELD_COUNT, row.length);
            assertEquals("30000", row[13]);
        });

        test("toCsvRow keeps decimals when present", () -> {
            EmployeeData e = sample("1", "Cruz", "Ana", "");
            e.setBasicSalary(1234.5);
            assertEquals("1234.5", e.toCsvRow()[13]);
        });

        test("fromCsvRow(toCsvRow()) round-trips", () -> {
            EmployeeData original = sample("77", "Reyes", "Mark", "Garcia, Manuel III");
            EmployeeData copy = EmployeeData.fromCsvRow(original.toCsvRow());
            assertArrayEquals(original.toCsvRow(), copy.toCsvRow());
        });

        test("getFullName and toString", () -> {
            EmployeeData e = sample("5", "Cruz", "Ana", "");
            assertEquals("Ana Cruz", e.getFullName());
            assertEquals("5 - Cruz, Ana (Payroll Clerk)", e.toString());
        });
    }

    // =====================================================================
    // CsvHandler: generic file operations
    // =====================================================================

    private static void fileTests() {
        test("writeAll then readAll returns the rows without the header", () -> {
            Path f = tempDir.resolve("generic.csv");
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[] { "1", "a, b" });
            rows.add(new String[] { "2", "" });
            CsvHandler.writeAll(f, new String[] { "id", "text" }, rows);

            List<String[]> read = CsvHandler.readAll(f);
            assertEquals(2, read.size());
            assertArrayEquals(new String[] { "1", "a, b" }, read.get(0));
            assertArrayEquals(new String[] { "2", "" }, read.get(1));
            assertArrayEquals(new String[] { "id", "text" }, CsvHandler.readHeader(f));
        });

        test("writeAll leaves no .tmp file behind", () -> {
            Path f = tempDir.resolve("clean.csv");
            CsvHandler.writeAll(f, new String[] { "id" }, new ArrayList<>());
            check(Files.exists(f), "target file should exist");
            check(!Files.exists(tempDir.resolve("clean.csv.tmp")), "temp file should be gone");
        });

        test("writeAll creates missing parent folders", () -> {
            Path f = tempDir.resolve("nested").resolve("deeper").resolve("x.csv");
            CsvHandler.writeAll(f, new String[] { "id" }, new ArrayList<>());
            check(Files.exists(f), "file should exist in the new folder");
        });

        test("readAll skips blank lines", () -> {
            Path f = tempDir.resolve("blanks.csv");
            Files.write(f, Arrays.asList("id,name", "", "1,Ana", "   ", "2,Ben", ""),
                    StandardCharsets.UTF_8);
            assertEquals(2, CsvHandler.readAll(f).size());
        });

        test("readAll and readHeader handle a leading BOM (Excel export)", () -> {
            Path f = tempDir.resolve("bom.csv");
            Files.write(f, Arrays.asList("\uFEFFid,name", "1,Ana"), StandardCharsets.UTF_8);
            assertEquals(1, CsvHandler.readAll(f).size());
            assertEquals("id", CsvHandler.readHeader(f)[0]);
        });

        test("readHeader on an empty file returns no columns", () -> {
            Path f = tempDir.resolve("empty.csv");
            Files.write(f, new byte[0]);
            assertEquals(0, CsvHandler.readHeader(f).length);
        });

        test("appendToCSV adds a record and keeps the header", () -> {
            Path f = tempDir.resolve("append.csv");
            CsvHandler.writeAll(f, new String[] { "id", "name" }, new ArrayList<>());
            CsvHandler.appendToCSV(f.toString(), "1", "Ana");
            CsvHandler.appendToCSV(f.toString(), "2", "Ben");
            assertEquals(2, CsvHandler.readAll(f).size());
            assertArrayEquals(new String[] { "id", "name" }, CsvHandler.readHeader(f));
        });

        test("updateCSV replaces the matching row, false when missing", () -> {
            Path f = tempDir.resolve("update.csv");
            CsvHandler.writeAll(f, new String[] { "id", "name" }, new ArrayList<>());
            CsvHandler.appendToCSV(f.toString(), "1", "Ana");
            CsvHandler.appendToCSV(f.toString(), "2", "Ben");

            check(CsvHandler.updateCSV(f.toString(), "2", "2", "Benjamin"), "should find id 2");
            check(!CsvHandler.updateCSV(f.toString(), "99", "99", "Nobody"), "id 99 should not exist");
            assertEquals("Benjamin", CsvHandler.readAll(f).get(1)[1]);
            assertEquals("Ana", CsvHandler.readAll(f).get(0)[1]);
        });

        test("deleteFromCSV removes the matching row, false when missing", () -> {
            Path f = tempDir.resolve("delete.csv");
            CsvHandler.writeAll(f, new String[] { "id", "name" }, new ArrayList<>());
            CsvHandler.appendToCSV(f.toString(), "1", "Ana");
            CsvHandler.appendToCSV(f.toString(), "2", "Ben");

            check(CsvHandler.deleteFromCSV(f.toString(), "1"), "should delete id 1");
            check(!CsvHandler.deleteFromCSV(f.toString(), "1"), "id 1 is already gone");
            List<String[]> left = CsvHandler.readAll(f);
            assertEquals(1, left.size());
            assertEquals("2", left.get(0)[0]);
        });

        test("key matching is exact, not a prefix match", () -> {
            Path f = tempDir.resolve("exact.csv");
            CsvHandler.writeAll(f, new String[] { "id", "name" }, new ArrayList<>());
            CsvHandler.appendToCSV(f.toString(), "10001", "Ana");
            check(!CsvHandler.deleteFromCSV(f.toString(), "1000"), "'1000' must not match '10001'");
            assertEquals(1, CsvHandler.readAll(f).size());
        });
    }

    // =====================================================================
    // CsvHandler: employee.csv operations
    // =====================================================================

    private static void employeeFileTests() {
        test("saveEmployee then loadEmployees finds the new employee", () -> {
            Path f = newEmployeeFile("emp-save.csv", sample("1", "Cruz", "Ana", ""));
            CsvHandler.saveEmployee(f, sample("2", "Reyes", "Ben", "Cruz, Ana"));
            List<EmployeeData> all = CsvHandler.loadEmployees(f);
            assertEquals(2, all.size());
            assertEquals("Reyes", all.get(1).getLastName());
        });

        test("saveEmployee rejects a duplicate empNo and writes nothing", () -> {
            Path f = newEmployeeFile("emp-dup.csv", sample("1", "Cruz", "Ana", ""));
            assertThrows(IllegalArgumentException.class,
                    () -> CsvHandler.saveEmployee(f, sample("1", "Other", "Person", "")));
            assertEquals(1, CsvHandler.loadEmployees(f).size());
        });

        test("updateEmployee changes the row, false when missing", () -> {
            Path f = newEmployeeFile("emp-upd.csv", sample("1", "Cruz", "Ana", ""));
            EmployeeData changed = sample("1", "Cruz", "Ana", "");
            changed.setBasicSalary(45000);
            check(CsvHandler.updateEmployee(f, changed), "should update id 1");
            check(!CsvHandler.updateEmployee(f, sample("99", "No", "Body", "")), "id 99 missing");
            assertEquals(45000.0, CsvHandler.loadEmployees(f).get(0).getBasicSalary());
        });

        test("deleteEmployee removes the row, false when missing", () -> {
            Path f = newEmployeeFile("emp-del.csv",
                    sample("1", "Cruz", "Ana", ""), sample("2", "Reyes", "Ben", ""));
            check(CsvHandler.deleteEmployee(f, "1"), "should delete id 1");
            check(!CsvHandler.deleteEmployee(f, "1"), "already deleted");
            List<EmployeeData> left = CsvHandler.loadEmployees(f);
            assertEquals(1, left.size());
            assertEquals("2", left.get(0).getEmpNo());
        });

        test("edits keep the 17-column header", () -> {
            Path f = newEmployeeFile("emp-header.csv", sample("1", "Cruz", "Ana", ""));
            CsvHandler.saveEmployee(f, sample("2", "Reyes", "Ben", ""));
            CsvHandler.deleteEmployee(f, "1");
            assertArrayEquals(HEADER, CsvHandler.readHeader(f));
        });

        test("loadEmployees skips a malformed row but keeps the good ones", () -> {
            Path f = newEmployeeFile("emp-bad.csv", sample("1", "Cruz", "Ana", ""));
            Files.write(f, Arrays.asList(CsvHandler.toCsvLine(HEADER),
                    CsvHandler.toCsvLine(sample("1", "Cruz", "Ana", "").toCsvRow()),
                    "2,only,three",
                    CsvHandler.toCsvLine(sample("3", "Lim", "Cara", "").toCsvRow())),
                    StandardCharsets.UTF_8);
            List<EmployeeData> all = CsvHandler.loadEmployees(f);
            assertEquals(2, all.size());
            assertEquals("3", all.get(1).getEmpNo());
        });

        test("getLastEmployeeNumber returns the highest number", () -> {
            Path f = newEmployeeFile("emp-last.csv",
                    sample("10001", "A", "A", ""), sample("10034", "B", "B", ""),
                    sample("10007", "C", "C", ""));
            assertEquals(10034, CsvHandler.getLastEmployeeNumber(f.toString()));
        });

        test("getLastEmployeeNumber returns 0 for an empty file", () -> {
            Path f = newEmployeeFile("emp-none.csv");
            assertEquals(0, CsvHandler.getLastEmployeeNumber(f.toString()));
        });

        test("getLastEmployeeNumber ignores a non-numeric empNo", () -> {
            Path f = tempDir.resolve("emp-nonnum.csv");
            CsvHandler.writeAll(f, new String[] { "empNo", "name" }, new ArrayList<>());
            CsvHandler.appendToCSV(f.toString(), "abc", "x");
            CsvHandler.appendToCSV(f.toString(), "12", "y");
            assertEquals(12, CsvHandler.getLastEmployeeNumber(f.toString()));
        });
    }

    // =====================================================================
    // CsvHandler: supervisor, attendance months, table model
    // =====================================================================

    private static void lookupTests() {
        test("findSupervisor matches 'Last, First' text", () -> {
            EmployeeData boss = sample("1", "Garcia", "Manuel III", "");
            EmployeeData worker = sample("2", "Cruz", "Ana", "Garcia, Manuel III");
            Optional<EmployeeData> found = CsvHandler.findSupervisor(worker, Arrays.asList(boss, worker));
            check(found.isPresent(), "supervisor should be found");
            assertEquals("1", found.get().getEmpNo());
        });

        test("findSupervisor ignores case, commas and spaces", () -> {
            EmployeeData boss = sample("1", "San Jose", "Brad", "");
            EmployeeData worker = sample("2", "Cruz", "Ana", "San, Jose Brad");
            check(CsvHandler.findSupervisor(worker, Arrays.asList(boss, worker)).isPresent(),
                    "'San, Jose Brad' should match last name 'San Jose'");

            EmployeeData shouting = sample("3", "Dela", "Rosa", "GARCIA,MANUEL III");
            EmployeeData ceo = sample("4", "Garcia", "Manuel III", "");
            check(CsvHandler.findSupervisor(shouting, Arrays.asList(ceo, shouting)).isPresent(),
                    "case and spacing should not matter");
        });

        test("findSupervisor returns empty for the CEO (blank supervisor)", () -> {
            EmployeeData ceo = sample("1", "Garcia", "Manuel III", "");
            check(!CsvHandler.findSupervisor(ceo, Arrays.asList(ceo)).isPresent(),
                    "CEO has no supervisor");
        });

        test("findSupervisor returns empty when nobody matches", () -> {
            EmployeeData worker = sample("2", "Cruz", "Ana", "Nobody, Known");
            check(!CsvHandler.findSupervisor(worker, Arrays.asList(worker)).isPresent(),
                    "no matching supervisor");
        });

        test("loadAvailableMonths returns distinct yyyy-MM values, sorted", () -> {
            Path f = tempDir.resolve("attendance.csv");
            CsvHandler.writeAll(f, new String[] { "empNo", "date", "logIn", "logOut" }, new ArrayList<>());
            CsvHandler.appendToCSV(f.toString(), "1", "2024-07-01", "8:00", "17:00");
            CsvHandler.appendToCSV(f.toString(), "1", "2024-06-03", "8:05", "17:00");
            CsvHandler.appendToCSV(f.toString(), "2", "2024-06-04", "8:00", "17:10");
            CsvHandler.appendToCSV(f.toString(), "2", "2024-12-02", "9:00", "18:00");
            assertEquals(Arrays.asList("2024-06", "2024-07", "2024-12"),
                    CsvHandler.loadAvailableMonths(f.toString()));
        });

        test("loadAvailableMonths on a header-only file is empty", () -> {
            Path f = tempDir.resolve("attendance-empty.csv");
            CsvHandler.writeAll(f, new String[] { "empNo", "date", "logIn", "logOut" }, new ArrayList<>());
            assertEquals(0, CsvHandler.loadAvailableMonths(f.toString()).size());
        });

        test("loadEmployeeData fills a model that has no columns yet", () -> {
            Path f = newEmployeeFile("emp-table.csv",
                    sample("1", "Cruz", "Ana", ""), sample("2", "Reyes", "Ben", ""));
            DefaultTableModel model = new DefaultTableModel();
            CsvHandler.loadEmployeeData(f.toString(), model);
            assertEquals(7, model.getColumnCount());
            assertEquals(2, model.getRowCount());
            assertEquals("Cruz", model.getValueAt(0, 1));
        });

        test("loadEmployeeData clears old rows before loading", () -> {
            Path f = newEmployeeFile("emp-table2.csv", sample("1", "Cruz", "Ana", ""));
            DefaultTableModel model = new DefaultTableModel();
            CsvHandler.loadEmployeeData(f.toString(), model);
            CsvHandler.loadEmployeeData(f.toString(), model);
            assertEquals(1, model.getRowCount());
        });
    }

    // =====================================================================
    // Real data (read-only). Skipped if the file is not found.
    // Run from the project root so the relative data/ path resolves.
    // =====================================================================

    private static void realDataTests() {
        test("data/employee.csv loads without skipping any row", () -> {
            if (!Files.exists(CsvHandler.EMPLOYEE_FILE)) {
                System.out.println("          (skipped: " + CsvHandler.EMPLOYEE_FILE
                        + " not found; run from the project root)");
                return;
            }
            int dataRows = CsvHandler.readAll(CsvHandler.EMPLOYEE_FILE).size();
            List<EmployeeData> loaded = CsvHandler.loadEmployees();
            check(!loaded.isEmpty(), "should load at least one employee");
            assertEquals(dataRows, loaded.size());
        });

        test("every real employee has a unique empNo and a findable supervisor", () -> {
            if (!Files.exists(CsvHandler.EMPLOYEE_FILE))
                return;
            List<EmployeeData> all = CsvHandler.loadEmployees();
            long unique = all.stream().map(EmployeeData::getEmpNo).distinct().count();
            assertEquals((long) all.size(), unique);

            for (EmployeeData e : all) {
                if (e.getSupervisor().isEmpty())
                    continue; // the CEO
                check(CsvHandler.findSupervisor(e, all).isPresent(),
                        "no supervisor match for " + e + " -> '" + e.getSupervisor() + "'");
            }
        });
    }

    // =====================================================================
    // Cleanup
    // =====================================================================

    private static void deleteRecursively(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
            // best effort: the OS clears the temp folder eventually
        }
    }
}
