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
        KICKING,
        BOXING,
        STRENGTH,
        RUNNING_SPEED
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
    Map <Skill, Integer> atackSkill = new HashMap<>();
    Map <Skill, Integer> defenseSkill = new HashMap<>();
    
    public void setAtackSkill(Skill skill, int value) {
        atackSkill.put(skill, value);
    }
    
    public void setDefenseSkill(Skill skill, int value) {
        defenseSkill.put(skill, value);
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
