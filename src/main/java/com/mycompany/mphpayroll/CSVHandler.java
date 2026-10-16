/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mphpayroll;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Jae
 */
public class CSVHandler {
    
    private final String csvFile; //encapsulation
    
    //Constractor
    public CSVHandler(String csvFile){
        this.csvFile = csvFile;
    }
    
    //Method
     public List<String[]> readAll() throws IOException, CsvException {

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            return reader.readAll();
        }
    }
    
}
