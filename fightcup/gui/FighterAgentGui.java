/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fightcup.gui;

import fightcup.FighterAgent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

/**
 *
 * @author Przemo
 */
public class FighterAgentGui extends JFrame {

    enum SkillMode {

        ATACK,
        DEFENSE
    };
    
    javax.swing.JSpinner atackBoxingSpinner = new javax.swing.JSpinner();
    javax.swing.JSpinner atackKickingSpinner = new javax.swing.JSpinner();
    javax.swing.JSpinner atackRunningSpinner = new javax.swing.JSpinner();
    javax.swing.JSpinner defenseBoxingSpinner = new javax.swing.JSpinner();
    javax.swing.JSpinner defenseKickingSpinner = new javax.swing.JSpinner();
    javax.swing.JSpinner defenseRunningSpinner = new javax.swing.JSpinner();
    javax.swing.JButton generateRandomButton = new JButton("Random values");
    javax.swing.JButton startFightButton = new JButton("Fight !");
    
    
    FighterAgent agent;

    /**
     * lista zawodnikow
     */
    List<FighterAgent> teamFighters = new ArrayList<>();

    /**
     * Mapa element formularza - skill
     */
    Map<FighterAgent.Skill, JSpinner> skillElementMap = new HashMap<>();

    public FighterAgentGui(FighterAgent a) {
        super(a.getLocalName());
        agent = a;
        initComponents();

        // wypelnienie mapy        
        skillElementMap.put(FighterAgent.Skill.ATACK_BOXING, atackBoxingSpinner);
        skillElementMap.put(FighterAgent.Skill.ATACK_KICKING, atackKickingSpinner);
        skillElementMap.put(FighterAgent.Skill.ATACK_RUNNING_SPEED, atackRunningSpinner);

        skillElementMap.put(FighterAgent.Skill.DEFENSE_BOXING, defenseBoxingSpinner);
        skillElementMap.put(FighterAgent.Skill.DEFENSE_KICKING, defenseKickingSpinner);
        skillElementMap.put(FighterAgent.Skill.DEFENSE_RUNNING_SPEED, defenseRunningSpinner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                agent.doDelete();
            }
        });

        setResizable(false);

    }

    private void randomSkills() {
        Random generator = new Random();
        for (JSpinner spinner : skillElementMap.values()) {
            int value = generator.nextInt(10);
            spinner.setValue(value);
        }
    }

    @SuppressWarnings("unchecked")    
    private void initComponents() {
        
        atackBoxingSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10, 1));
        atackKickingSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10, 1));
        atackRunningSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10, 1));
        
        defenseBoxingSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10, 1));
        defenseKickingSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10, 1));
        defenseRunningSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10, 1));
        
        generateRandomButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                generateRandomButtonMouseClicked(evt);
            }
        });
        
        startFightButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                startFightButtonMouseClicked(evt);
            }
        });
        
        Container pane = this.getContentPane();
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(0,3));
        
        JPanel controls = new JPanel();
        pane.add(grid, BorderLayout.CENTER);
        pane.add(controls, BorderLayout.SOUTH);
 
        grid.add(new JLabel("Skill"));  
        grid.add(new JLabel("ATACK")); 
        grid.add(new JLabel("DEFENSE")); 
        
        grid.add(new JLabel("Boxing")); 
        grid.add(atackBoxingSpinner); 
        grid.add(defenseBoxingSpinner); 
        
        grid.add(new JLabel("Kicking")); 
        grid.add(atackKickingSpinner); 
        grid.add(defenseKickingSpinner); 
        
        grid.add(new JLabel("Running speed")); 
        grid.add(atackRunningSpinner); 
        grid.add(defenseRunningSpinner); 
        
        controls.add(generateRandomButton); 
        controls.add(startFightButton); 

    }                  

    private void generateRandomButtonMouseClicked(java.awt.event.MouseEvent evt) {                                      
        randomSkills();
    }  
    
    private void startFightButtonMouseClicked(java.awt.event.MouseEvent evt) {                                      
        agent.setMode(FighterAgent.Mode.FIGHTING);
    }

    public void showGui() {
        pack();
        super.setVisible(true);
    }
             
}
