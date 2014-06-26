/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fightcup;

import fightcup.gui.FighterAgentGui;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Przemo
 */
public class FighterAgent extends Agent {

    public enum Skill {
        ATACK_KICKING,
        ATACK_BOXING,
        ATACK_RUNNING_SPEED,
        ATACK_SPEED,
        DEFENSE_KICKING,
        DEFENSE_BOXING,
        DEFENSE_RUNNING_SPEED
    }
    
    AgentParams params;

    Random randomizer = new Random();

    /**
     * Przeciwnik
     */
    DFAgentDescription oponent;

    /**
     * Grupa do jakiej nalezy agent
     */
    String group;

    /**
     * GUI
     */
    FighterAgentGui gui;

    /**
     * Zdrowie zawodnika w jednostkach
     */
    int health;

    /**
     * Poziom zdrowia zawodnika podczas ostatniego wolania pomocy
     */
    int lastHelpAskHealthValue = 0;

    /**
     * Wartosci umiejetnosci dla ataku i obrony
     */
    Map<Skill, Integer> skills = new HashMap<>();

    /**
     * Lista przeciwnikow z ktorymi aktualnie walczy
     */
    List<AID> oponents = new ArrayList<AID>();

    DFAgentDescription dfd = new DFAgentDescription();
    
    @Override
    protected void setup() {
        
        skills = getInitialSkillValues();
        Object[] args = this.getArguments();
        if (args != null && args.length > 1) {
            params = new AgentParams(this, String.valueOf(args[0]));
            group = String.valueOf(args[1]);
            gui = new FighterAgentGui(this, skills);
            gui.setTitle(this.getLocalName() + " from team: " + params.getTeam() + "/" + group);
            gui.showGui();
            
            health = getStartupHealth();
            System.out.println(getLocalName() +" health "  + health);

            dfd.setName(getAID());
            // rejestracja agenta jako fightera
            addServiceDescription("fighter");
            // rejestracja uslugi wymiany ciosow
            addServiceDescription("blow");
            // rejestracja uslugi pomocy zawodnikom swojej druzyny
            addServiceDescription("help" + params.getTeam());

            try {
                DFService.register(this, dfd);
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            /**
             * Odpowiedz na propozycje walki
             */
            addBehaviour(new FightRequestService());

            /**
             * Odpowiedz na atak przeciwnika
             */
            addBehaviour(new AtackReceiveService());

        } else {
            System.err.println("Podaj druzyne i grupe dla agenta");
            this.doDelete();
        }
    }
    
    private Map<Skill, Integer> getInitialSkillValues() {
        Map<Skill, Integer> s = new HashMap<Skill, Integer>();
        for (Skill skill : Skill.values()) {
            s.put(skill, randomizer.nextInt(10) + 1);
        }
        return s;
    }

    public void setSkill(Skill skill, int value) {
        skills.put(skill, value);
    }

    public int getSkill(Skill skill) {
        return skills.get(skill);
    }

    /**
     * Zdrowie zawodnika na starcie, wyliczane na podstawie jego zdolnosci
     * obrony
     *
     * @return int
     */
    private int getStartupHealth() {
        int h = getSkill(Skill.DEFENSE_BOXING);
        h += getSkill(Skill.DEFENSE_KICKING);
        h += getSkill(Skill.DEFENSE_RUNNING_SPEED);
        return h * 10;
    }

    /**
     * Szuka chetnego do walki
     */
    public void findSomeoneToFight() {
        params.setMode(AgentParams.Mode.SEARCHING_OPONENTS);
        
        /**
         * Szukamy agentow z usluga walki
         */
        System.out.println(getLocalName() + " searching for oponents");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("fighter");
        template.addServices(sd);
        // szukamy agentow do walki
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                System.out.println(getLocalName() + " founds the following potential oponents");
                List<AID> oponentsAgents = new ArrayList<>();
                for (DFAgentDescription result1 : result) {
                    // bez sensu walczyc z samym soba 
                    if (!result1.getName().equals(FighterAgent.this.getAID())) {
                        oponentsAgents.add(result1.getName());
                        System.out.println(result1.getName());
                    }
                }
                // pytamy czy chca walczyc
                addBehaviour(new FightRequestPerformer(oponentsAgents));
            } else {
                System.out.println(getLocalName() + " there was no oponents to fight");
            }

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /**
     * Cios wymierzony przez przeciwnika
     *
     * @param oponentSkill
     * @param blowStrength
     */
    public void blowFromOponent(Skill oponentSkill, int blowStrength, AID oponent) {
        
        /**
         * Moc ciosu przeciwnika jest wyliczana jako roznica między jego siła
         * obrony a siła ciosu przeciwnika Nie moze byc mniejsza niz 1 -
         * zakladamy ze kazdy cios oslabia
         */
        int myDefenseSkillValue;
        if (oponentSkill == Skill.ATACK_BOXING) {
            myDefenseSkillValue = getSkill(Skill.DEFENSE_BOXING);
        } else if (oponentSkill == Skill.ATACK_KICKING) {
            myDefenseSkillValue = getSkill(Skill.DEFENSE_KICKING);
        } else {
            myDefenseSkillValue = 0;
        }

        blowStrength -= myDefenseSkillValue;
        
        
        // cios byl slabszy niz ochrona przed nim
        if (blowStrength < 1) {
            blowStrength = 1;
        }
        System.out.println(getLocalName() + " dostal w pape od " + oponent.getLocalName() + " " +oponentSkill.name() + " " + blowStrength);
        updateHealth(blowStrength * -1);

        // fighter nigdy nie jest ciamajda, jak dostaje cios zaraz zaczyna walke
        if (params.getMode() == AgentParams.Mode.WAITING_FOR_FIGHT) {
            // odbija atak
            startAtack(oponent);
        }

    }

    private void updateHealth(int value) {
        health += value;
        
        double healthPercent = (health * 1.0) / (getStartupHealth() * 1.0);
        System.out.println("Current health " + health + " / "+getStartupHealth()+ " = "+healthPercent);
        if (healthPercent < 0.1) {
            params.setMode(AgentParams.Mode.DEAD);
        } /**
         * Jesli zycie spadnie ponizej 50% zawodnik zaczyna wolac o pomoc,
         * wysyla komunikat help do swojej druzyny po kazdym spadku zycia o 10%
         */
        else if (health / getStartupHealth() < 0.5) {
            // wartosc skoku zycia o 10%
            long helpAskStep = Math.round(getStartupHealth() * 0.01);
            if (lastHelpAskHealthValue == 0 || health + helpAskStep < lastHelpAskHealthValue) {
                askForHelp();
            }
        }
        System.out.println(getLocalName() + " health: " + health);
    }

    private void askForHelp() {
        lastHelpAskHealthValue = health;
        /**
         * Szukamy ziomkow ktorzy moga pomoc
         */
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("help" + params.getTeam());
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                // wysylamy im komunikat pomocy
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setConversationId("help" + params.getTeam());
                /**
                 * @TODO tutaj trzeba pewnie wyslac swoje wspolrzedne zeby
                 * wiedzieli gdzie isc
                 */
                //msg.setContentObject();
                msg.setReplyWith("help" + params.getTeam() + System.currentTimeMillis()); //unikalna wartosc
                for (DFAgentDescription result1 : result) {
                    // samego siebie odrzucamy
                    if (!result1.getName().equals(FighterAgent.this.getAID())) {
                        msg.addReceiver(result1.getName());
                    }
                }
                send(msg);

            } else {
                System.out.println(getLocalName() + " there was no oponents to fight");
            }

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /**
     * Rozpoczyna atak na przeciwnikow
     */
    private void startAtack(AID oponent) {
        System.out.println(getLocalName() + " zaczal atak");
        params.setMode(AgentParams.Mode.FIGHTING);
        oponents.add(oponent);
        // Czestotliwosc ataku uzalezniona od skilla ATACK_SPEED max 10, im wiecej tym lepiej
        int speed = 11 - getSkill(Skill.ATACK_SPEED);

        addBehaviour(new AtackPerformer(this, speed * 1000, oponents));
    }



    private void addServiceDescription(String name) {
        ServiceDescription sdFighter = new ServiceDescription();
        sdFighter.setType(name);
        sdFighter.setName("JADE" + name);
        dfd.addServices(sdFighter);
    }

    private class FightRequestPerformer extends CyclicBehaviour {

        List<AID> oponentsAgents;
        List<AID> fighters = new ArrayList<>();
        int step = 0;
        int replyCounter = 0;
        MessageTemplate mt;

        public FightRequestPerformer(List<AID> o) {
            oponentsAgents = o;
        }

        @Override
        public void action() {
            try {
                switch (step) {
                    case 0:
                        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                        msg.setConversationId("fighter");
                        msg.setContentObject(params);
                        msg.setReplyWith("fighter" + System.currentTimeMillis()); //unikalna wartosc
                        for (AID agent : oponentsAgents) {
                            msg.addReceiver(agent);
                        }
                        send(msg);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("fighter"),
                                MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                        step = 1;
                        break;
                    case 1:
                        ACLMessage reply = myAgent.receive(mt);
                        if (reply != null) {

                            replyCounter++;
                            // zgoda na propozycje walki
                            if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                                System.out.println(reply.getSender().getLocalName() + " want to fight");
                                fighters.add(reply.getSender());
                            }
                            if (replyCounter == oponentsAgents.size()) {
                                // otrzymano wszystkie odpowiedzi losujemy przeciwnika
                                if (fighters.size() > 0) {
                                    AID oponent = fighters.get(randomizer.nextInt(fighters.size()));
                                    System.out.println(myAgent.getLocalName() + " vs. " + oponent.getLocalName());
                                    startAtack(oponent);
                                }
                                step = 2;
                            }
                        } else {
                            block();
                        }
                        break;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class FightRequestService extends CyclicBehaviour {

        // tylko propozycje walki
        private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("fighter"),
                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
                
        @Override
        public void action() {
            try {
                
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    AgentParams oponentParams = (AgentParams)msg.getContentObject();
                    System.out.println(myAgent.getLocalName() + " receive fight proposal from team " + oponentParams.getTeam());
                    ACLMessage reply = msg.createReply();
                    // akceptuj jesli propozycja z przeciwnej druzyny
                    if (!oponentParams.getTeam().equals(params.getTeam()) && params.getMode() == AgentParams.Mode.WAITING_FOR_FIGHT) {
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        try {
                            msg.setContentObject(params);
                        } catch (IOException ex) {
                            Logger.getLogger(FighterAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("same-team");
                    }
                    myAgent.send(reply);
                } else {
                    block();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class AtackPerformer extends TickerBehaviour {

        List<AID> oponents;
        List<Skill> atackSkills = Arrays.asList(Skill.ATACK_BOXING, Skill.ATACK_KICKING);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        Random randomizer = new Random();

        public AtackPerformer(Agent a, long period, List<AID> op) {
            super(a, period);
            oponents = op;
            msg.setConversationId("blow");
        }

        @Override
        protected void onTick() {
            try {
                // losowy sklill
                Skill skill = atackSkills.get(randomizer.nextInt(atackSkills.size()));
                // losowa jego sila z przedzialu 0 (mogl nie trafic :)) do max skill level
                Integer value = randomizer.nextInt(skills.get(skill) + 1);

                // losowy przeciwnik z listy
                if (oponents.size() > 0) {
                    int i =randomizer.nextInt(oponents.size());
                    System.out.println(i);
                    AID oponent = oponents.get(i);

                    // wysylamy cios do przeciwnika wylosowanego z listy przeciwnikow  
                    msg.setContentObject(new BlowObject(skill, value));
                    msg.setReplyWith("blow" + System.currentTimeMillis()); //unikalna wartosc
                    msg.addReceiver(oponent);
                    send(msg);
                    msg.removeReceiver(oponent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AtackReceiveService extends CyclicBehaviour {

        MessageTemplate mt = MessageTemplate.MatchConversationId("blow");

        @Override
        public void action() {
        
            if (params.getMode() == AgentParams.Mode.FIGHTING || params.getMode() == AgentParams.Mode.WAITING_FOR_FIGHT) {
                System.out.println("Status OK");
                try {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // dostal w pape
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            BlowObject oponentBlow = (BlowObject) reply.getContentObject();
                            blowFromOponent(oponentBlow.getSkill(), oponentBlow.getSkillValue(), reply.getSender());
                        }
                    } else {
                        block();
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

}
