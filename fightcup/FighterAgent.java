/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fightcup;

import fightcup.gui.FighterAgentGui;
import jade.core.Agent;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Przemo
 */
public class FighterAgent extends Agent {
    public enum Mode {
        FIGHTING,
        RUNING_AWAY,
        WAITING_FOR_FIGHT
    }
    public enum Skill {
        ATACK_KICKING,
        ATACK_BOXING,
        ATACK_RUNNING_SPEED,
        DEFENSE_KICKING,
        DEFENSE_BOXING,
        DEFENSE_RUNNING_SPEED
    }
    
    /**
     * GUI
     */
    FighterAgentGui gui;
    
    /**
     * Zdrowie zawodnika, inicjalnie 100%
     */
    int health = 100;
    
    /**
     * Tryb w jakim aktualnie znajduje sie zawodnik
     */
    Mode mode = Mode.WAITING_FOR_FIGHT;
    
    /**
     * Wartosci umiejetnosci dla ataku i obrony
     */
    Map <Skill, Integer> skills = new HashMap<>();
    
    public void setSkill(Skill skill, int value) {
        skills.put(skill, value);
    }
    
    public int getSkill(Skill skill) {
        return skills.get(skill);
    }
    
    public void setMode(Mode m) {
        mode = m;
    }
    
    /**
     * Cios wymierzony przez przeciwnika
     * 
     * @param oponentSkill
     * @param blowStrength 
     */
    public void blowFromOponent(Skill oponentSkill, int blowStrength) {
        // fighter nigdy nie jest ciamajda, jak dostaje cios zaraz zaczyna walke
        if (mode == Mode.WAITING_FOR_FIGHT) {
            setMode(Mode.FIGHTING);
        }
        
    }
    
    private void updateHealth(int value) {
        health += value;
        if (health < 50) {
            mode = Mode.RUNING_AWAY;
        }
    }
    
    @Override
    protected void setup() {
        gui = new FighterAgentGui(this);
        Object[] args = this.getArguments();
        if (args != null && args.length > 0) {
            String team = String.valueOf(args[0]);
            gui.setTitle(this.getLocalName() + " from team: " + team);
            gui.showGui();
        }
        else {
            System.err.println("Podaj druzyne dla agenta (pierwszy parametr)");
            this.doDelete();
        } 
    }
    
            
}
