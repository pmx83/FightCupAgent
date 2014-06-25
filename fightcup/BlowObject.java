package fightcup;

import java.io.Serializable;

/**
 *
 * @author Przemo
 */
public class BlowObject implements Serializable {
    
    private final FighterAgent.Skill skill;
    private final int skillValue;
    
    public BlowObject(FighterAgent.Skill skill, int skillValue) {
        this.skill = skill;
        this.skillValue = skillValue;
    }
    
    public final FighterAgent.Skill getSkill() {
        return this.skill;
    }
    
    public final int getSkillValue() {
        return this.skillValue;
    }
}
