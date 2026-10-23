/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package others;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Jae
 */
public class currentDate {
    
    public String getDate(){
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. dd,yyyy");
        
        return LocalDate.now().format(formatter);
        
    }
    
   
}
