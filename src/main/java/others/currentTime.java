/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package others;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Jae
 */
public class currentTime {
    
    public String getTime(){
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        
        return LocalTime.now().format(formatter);
        
        
    }
    
    
}
