/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fightcup;

import fightcup.gui.FighterAgentGui;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Przemo
 */
public class FighterAgent extends Agent {

    public enum Mode {

        FIGHTING,
        RUNING_AWAY,
        WAITING_FOR_FIGHT,
        SEARCHING_OPONENTS
    }

    public enum Skill {

        ATACK_KICKING,
        ATACK_BOXING,
        ATACK_RUNNING_SPEED,
        ATACK_SPEED,
        DEFENSE_KICKING,
        DEFENSE_BOXING,
        DEFENSE_RUNNING_SPEED
    }
    
    Random randomizer = new Random();

    /**
     * Przeciwnik
     */
    DFAgentDescription oponent;

    /**
     * Druzyna i grupa do jakiej nalezy agent
     */
    String team;

    String group;

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
    Map<Skill, Integer> skills = new HashMap<>();

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
     * Szuka chetnego do walki
     */
    public void findSomeoneToFight() {
        setMode(Mode.SEARCHING_OPONENTS);
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
        // fighter nigdy nie jest ciamajda, jak dostaje cios zaraz zaczyna walke
        if (mode == Mode.WAITING_FOR_FIGHT) {
            // odbija atak
            startAtack(oponent);
        }

    }
    
    private void updateHealth(int value) {
        health += value;
        if (health < 50) {
            mode = Mode.RUNING_AWAY;
        }
    }

    /**
     * Rozpoczyna atak na przeciwnika
     * @param oponent 
     */
    private void startAtack(AID oponent) {
        setMode(Mode.FIGHTING);
        // Czestotliwosc ataku uzalezniona od skilla ATACK_SPEED max 10, im wiecej tym lepiej
        int speed = 11 - getSkill(Skill.ATACK_SPEED);
        
        addBehaviour(new AtackPerformer(this, speed * 1000, oponent));
    }

    @Override
    protected void setup() {
        gui = new FighterAgentGui(this);
        Object[] args = this.getArguments();
        if (args != null && args.length > 1) {
            team = String.valueOf(args[0]);
            group = String.valueOf(args[1]);
            gui.setTitle(this.getLocalName() + " from team: " + team + "/" + group);
            gui.showGui();

            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            // rejestracja agenta jako fightera
            ServiceDescription sdFighter = new ServiceDescription();
            sdFighter.setType("fighter");
            sdFighter.setName("JADE-fighter");
            dfd.addServices(sdFighter);
            // rejestracja uslugi wymiany ciosow
            ServiceDescription sdBlow = new ServiceDescription();
            sdBlow.setType("blow");
            sdBlow.setName("JADE-blow");
            dfd.addServices(sdBlow);

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

    private class FightRequestPerformer extends CyclicBehaviour {

        List<AID> oponentsAgents;
        List<AID> fighters = new ArrayList<>();
        int step = 0;
        int replyCounter = 0;
        MessageTemplate mt;

        public FightRequestPerformer(List<AID> oponents) {
            oponentsAgents = oponents;
        }

        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                    msg.setConversationId("fighter");
                    msg.setContent(team);
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
                        if (mode == Mode.WAITING_FOR_FIGHT && reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            System.out.println(reply.getSender().getLocalName() + " zgodzil sie na walke");
                            fighters.add(reply.getSender());
                        }
                        if (replyCounter == oponentsAgents.size()) {
                            // otrzymano wszystkie odpowiedzi losujemy przeciwnika
                            AID oponent = fighters.get(randomizer.nextInt(fighters.size() - 1));
                            System.out.println(myAgent.getLocalName() + " vs. " + oponent.getLocalName());
                            startAtack(oponent);

                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

    }

    private class FightRequestService extends CyclicBehaviour {

        @Override
        public void action() {
            // tylko propozycje walki
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("fighter"),
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String oponentTeam = msg.getContent();
                System.out.println(myAgent.getLocalName() + " receive fight proposal from team " + oponentTeam);
                ACLMessage reply = msg.createReply();
                // akceptuj jesli propozycja z przeciwnej druzyny
                if (!oponentTeam.equals(team) && mode == Mode.WAITING_FOR_FIGHT) {
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    reply.setContent(team);
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("same-team");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class AtackPerformer extends TickerBehaviour {
        
        AID oponent;
        List<Skill> atackSkills = Arrays.asList(Skill.ATACK_BOXING, Skill.ATACK_KICKING);

        public AtackPerformer(Agent a, long period, AID op) {
            super(a, period);
            oponent = op;
        }

        @Override
        protected void onTick() {    
            try {
                // losowy sklill
                Skill skill = atackSkills.get(randomizer.nextInt(atackSkills.size() - 1));
                // losowa jego sila z przedzialu 0 (mogl nie trafic :)) do max skill level
                Integer value = randomizer.nextInt(skills.get(skill));

                // wysylamy cios do przeciwnika
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setConversationId("blow");
                msg.setContentObject(new BlowObject(skill, value));
                msg.setReplyWith("blow" + System.currentTimeMillis()); //unikalna wartosc
                msg.addReceiver(oponent);

                send(msg);       
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } 
    }
    
    private class AtackReceiveService extends CyclicBehaviour {

        MessageTemplate mt = MessageTemplate.MatchConversationId("blow");
        
        @Override
        public void action() {
            if (mode == Mode.FIGHTING || mode == Mode.WAITING_FOR_FIGHT) {
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
                }
                catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
            else {
                block();
            }
        }
        
    }
}
