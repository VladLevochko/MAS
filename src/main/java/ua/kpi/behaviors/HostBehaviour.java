package ua.kpi.behaviors;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.agents.Citizen;
import ua.kpi.properties.CitizenState;

import java.io.IOException;

public class HostBehaviour extends CyclicBehaviour {
    private final long WAIT_GUEST_TIME = 5000;

    private Citizen agent;
    private AID guest;
    private long timeToBecomeFree;

    public HostBehaviour(Citizen agent) {
        this.agent = agent;
    }

    @Override
    public void action() {
        MessageTemplate template = MessageTemplate.or(
                MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM)),
                MessageTemplate.MatchPerformative(ACLMessage.CANCEL)
        );
        ACLMessage message = agent.receive(template);
        if (message != null) {
            processMessage(message);
        } else {
            block();
        }
    }

    private void processMessage(ACLMessage message) {
        if (agent.getCitizenState().getValue() == CitizenState.State.WITH_GUEST
                && timeToBecomeFree <= System.currentTimeMillis()) {
            agent.getCitizenState().setValue(CitizenState.State.AT_HOME);
        }

        if (agent.getCitizenState().isAtHome()) {
            homeBehaviour(message);
        } else {
            outOfHomeBehaviour(message);
        }
    }

    private void homeBehaviour(ACLMessage message) {
        switch (message.getPerformative()) {
            case ACLMessage.PROPOSE:
//                MyLog.log(agent + " get proposition from " + message.getSender().getLocalName());
                try {
                    ACLMessage response = message.createReply();
                    response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    response.setContentObject(agent.getLocation());

                    agent.send(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                agent.getCitizenState().setValue(CitizenState.State.WAITING_FOR_GUEST);
//                MyLog.log(agent + " is waiting for guest");
                guest = message.getSender();

                agent.addBehaviour(new TickerBehaviour(agent, WAIT_GUEST_TIME) {
                    private AID waitingFor = message.getSender();

                    @Override
                    protected void onTick() {
                        if (agent.getCitizenState().getValue() == CitizenState.State.WAITING_FOR_GUEST
                                && guest.equals(waitingFor)) {
//                            MyLog.log(agent + " resetting state to AT_HOME");
                            agent.getCitizenState().setValue(CitizenState.State.AT_HOME);
                        }
                    }
                });

                break;

            case ACLMessage.INFORM:
                agent.getCitizenState().setValue(CitizenState.State.WITH_GUEST);
                long timeForGuest = Long.parseLong(message.getContent());
                MyLog.log(agent + " will be with guest for " + timeForGuest);
                timeToBecomeFree = System.currentTimeMillis() + timeForGuest * 1000 / Main.MODELLING_SPEED;
//                MyLog.log(String.format("%s: %s is leaving", agent, guest.getLocalName()));
            case ACLMessage.CANCEL:
//                MyLog.log(agent + " is ready to have guests");
                agent.getCitizenState().setValue(CitizenState.State.AT_HOME);
        }
    }

    private void outOfHomeBehaviour(ACLMessage message) {
        if (message.getPerformative() == ACLMessage.PROPOSE) {
            ACLMessage response = message.createReply();
            response.setPerformative(ACLMessage.REJECT_PROPOSAL);

            agent.send(response);
        }
    }
}
