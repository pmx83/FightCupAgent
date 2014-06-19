/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fightcup;

import java.awt.EventQueue;
import javax.swing.JFrame;

/**
 *
 * @author Przemo
 */
public class FightCup {
    
    public static String AppName = "FightCup";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                FightCup app = new FightCup();
                app.buildAndDisplayGui();
            }
        });
    }

    private void buildAndDisplayGui() {
        JFrame frame = new ParamsForm();
        frame.setTitle(AppName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
