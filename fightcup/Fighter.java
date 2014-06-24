/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fightcup;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Przemo
 */
public class Fighter {
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
    
    /**
     * Cios wymierzony przez przeciwnika
     * 
     * @param oponentSkill
     * @param blowStrength 
     */
    public void blowFromOponent(Skill oponentSkill, int blowStrength) {
        
    }
    
    private void updateHealth(int value) {
        health += value;
        if (health < 50) {
            mode = Mode.RUNING_AWAY;
        }
    }
            
}
