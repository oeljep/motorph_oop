/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package payrollhub.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AttendanceService {

    private final Path csvFile;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");

    public AttendanceService(String csvFilePath) {
        this.csvFile = Path.of(csvFilePath);
    }

    public void recordTimeIn(
            String empNo,
            LocalDateTime timeIn) throws IOException {

        boolean needsHeader = !Files.exists(csvFile)
                || Files.size(csvFile) == 0;

        try (var writer = Files.newBufferedWriter(
                csvFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            if (needsHeader) {
                writer.write("empNo,date,logIn,logOut");
                writer.newLine();
            }

            writer.write(
                    empNo + ","
                    + timeIn.format(DATE_FORMAT) + ","
                    + timeIn.format(TIME_FORMAT) + ","
            );

            writer.newLine();
        }
    }
}