package fightcup;

import java.io.Serializable;

/**
 *
 * @author Przemo
 */
public class AgentParams implements Serializable {

    public enum Mode {

        FIGHTING,
        WAITING_FOR_FIGHT,
        SEARCHING_OPONENTS,
        DEAD
    }
    
    private Mode mode = Mode.WAITING_FOR_FIGHT;
    private String name = null;
    private String team;
    
    public AgentParams(FighterAgent a, String t) {
        name = a.getName();
        team = t;
    }
    
    public void setMode(Mode m) {
        System.out.println(name + " "+ m.name());
        mode = m;
    }
    
    public Mode getMode() {
        return mode;
    }
    
    public String getTeam() {
        return team;
    }
}
