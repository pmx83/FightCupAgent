/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fightcup;

import java.awt.BorderLayout;
import java.awt.Container;
import javax.swing.JFrame;

/**
 *
 * @author Przemo
 */
public class ParamsForm extends JFrame {
    
    TeamParamsPanel teamPanelA = new TeamParamsPanel("Team A");
    TeamParamsPanel teamPanelB = new TeamParamsPanel("Team B");
    
    public ParamsForm() {
        super();
        initComponents();
       
    }
    
    private void initComponents() {
        Container pane = getContentPane();        
        
        pane.add(teamPanelA, BorderLayout.WEST);
        pane.add(teamPanelB, BorderLayout.EAST);
    }
}
