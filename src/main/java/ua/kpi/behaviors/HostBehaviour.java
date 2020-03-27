package ua.kpi.behaviors;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import ua.kpi.agents.Citizen;
import ua.kpi.properties.CitizenState;

import java.io.IOException;

public class HostBehaviour extends CyclicBehaviour {
    private Citizen agent;

    public HostBehaviour() {
        agent = (Citizen) myAgent;
    }

    @Override
    public void action() {
        ACLMessage message = agent.receive();
        if (message != null) {
            processMessage(message);
        } else {
            block();
        }
    }

    private void processMessage(ACLMessage message) {
        if (agent.getCitizenState().isAtHome()) {
            homeBehaviour(message);
        } else {
            outOfHomeBehaviour(message);
        }
    }

    private void homeBehaviour(ACLMessage message) {
        switch (message.getPerformative()) {
            case ACLMessage.PROPOSE:
                try {
                    ACLMessage response = message.createReply();
                    response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    response.setContentObject(agent.getLocation());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                agent.getCitizenState().setValue(CitizenState.State.WAIT_FOR_GUEST);

                break;

            case ACLMessage.INFORM:
                long timeForGuest = Long.parseLong(message.getContent());
                try {
                    Thread.sleep(timeForGuest);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case ACLMessage.CANCEL:
                agent.getCitizenState().setValue(CitizenState.State.AT_HOME);
        }
    }

    private void outOfHomeBehaviour(ACLMessage message) {
        if (message.getPerformative() == ACLMessage.PROPOSE) {
            ACLMessage response = message.createReply();
            response.setPerformative(ACLMessage.REJECT_PROPOSAL);

            agent.send(response);
        }
        // TODO: check if we have other types on incoming messages
    }
}
