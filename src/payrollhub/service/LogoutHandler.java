/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package payrollhub.service;

import javax.swing.JFrame;
import payrollhub.gui.GUILogin;

/**
 *
 * @author Jae
 */
public class LogoutHandler {

    public void logout(JFrame currentForm) {

        // open the Login GUI
        GUILogin loginForm = new GUILogin();
        loginForm.setVisible(true);

        currentForm.dispose();

    }

}
