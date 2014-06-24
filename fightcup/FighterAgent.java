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
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
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
        DEFENSE_KICKING,
        DEFENSE_BOXING,
        DEFENSE_RUNNING_SPEED
    }

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
        if (args != null && args.length > 1) {
            team = String.valueOf(args[0]);
            group = String.valueOf(args[1]);
            gui.setTitle(this.getLocalName() + " from team: " + team + "/" + group);
            gui.showGui();

            /**
             * Szukamy przeciwanikow
             */
            addBehaviour(new FindOponentBevavior(this, 1000));

            /**
             * rejestracja uslugi odpowiedzi na pytanie (cfp) czy walczymy
             */
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("ready-to-fight");
            sd.setName("JADE-ready-to-fight");
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            addBehaviour(new FightRequestsServer());

        } else {
            System.err.println("Podaj druzyne i grupe dla agenta");
            this.doDelete();
        }
    }

    /**
     * Szuka oponentow do walki
     */
    private class FindOponentBevavior extends TickerBehaviour {

        public FindOponentBevavior(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            /**
             * Jesli zawodnik jest w trybie szukania przeciwnika odpytuje
             * kazdego agenta czy chce walczyc
             */
            if (mode == Mode.SEARCHING_OPONENTS) {
                System.out.println(getLocalName() + " searching for oponents");
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("ready-to-fight");
                template.addServices(sd);
                // szukamy agentow do walki
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result.length > 0) {
                        System.out.println(getLocalName() + " founds the following potential oponents");
                        List<AID> oponentsAgents = new ArrayList<>();
                        for (int i = 0; i < result.length; i++) {
                            // bez sensu walczyc z samym soba samego siebie
                            if (!result[i].getName().equals(FighterAgent.this.getAID())) {
                                oponentsAgents.add(result[i].getName());
                                System.out.println(result[i].getName());
                            }
                        }
                        // pytamy czy chca walczyc
                        myAgent.addBehaviour(new FightRequestPerformer(oponentsAgents));
                    } else {
                        System.out.println(getLocalName() + " there was no oponents to fight");
                    }

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                setMode(Mode.WAITING_FOR_FIGHT);
            }
        }
    }

    /**
     * Rozglaszanie checi walki przeciwnika
     */
    private class FightRequestPerformer extends Behaviour {

        List<AID> oponents;
        
        public FightRequestPerformer(List<AID> ad) {
            oponents = ad;
        }

        @Override
        public void action() {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID agentAID : oponents) {
                cfp.addReceiver(agentAID);
            }
            // informujemy z jakiej druzyny jestesmy, szukamy gotowych do walki
            cfp.setContent(team);
            cfp.setConversationId("do-you-want-to-fight");
            cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unikalna wartosc
            myAgent.send(cfp);
        }

        @Override
        public boolean done() {
            return true;
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    /**
     * Odpowiedz na zaproszenie do walki
     */
    private class FightRequestsServer extends CyclicBehaviour {

        @Override
        public void action() {
            //tylko zawodnicy chcacy walczyc
            MessageTemplate mtCfp = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("do-you-want-to-fight"));
            ACLMessage msgCfp = myAgent.receive(mtCfp);
            
            MessageTemplate mtPropose = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchConversationId("do-you-want-to-fight"));
            ACLMessage msgPropose = myAgent.receive(mtPropose);
            
            
            if (msgCfp != null) {
                String team = msgCfp.getContent();
                ACLMessage reply = msgCfp.createReply();
                reply.setConversationId("do-you-want-to-fight");
                // zawodnicy czekajacy na walke i z innej druzyny
                if (mode == Mode.WAITING_FOR_FIGHT && !FighterAgent.this.team.equals(team)) {
                    // zawodnik startuje do walki
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(FighterAgent.this.team);
                } else {
                    // zawodnik z tej samej druzyny lub wlasnie walczy
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } 
            else if (msgPropose != null) {
                System.out.println("propose");
            } else {
                block();
            }
        }
    }

}
