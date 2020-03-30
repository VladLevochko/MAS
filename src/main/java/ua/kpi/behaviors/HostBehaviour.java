package ua.kpi.behaviors;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ua.kpi.MyLog;
import ua.kpi.agents.Citizen;
import ua.kpi.properties.CitizenState;

import java.io.IOException;

public class HostBehaviour extends CyclicBehaviour {
    private Citizen agent;

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
        if (agent.getCitizenState().isAtHome()) {
            homeBehaviour(message);
        } else {
            outOfHomeBehaviour(message);
        }
    }

    private void homeBehaviour(ACLMessage message) {
        switch (message.getPerformative()) {
            case ACLMessage.PROPOSE:
                MyLog.log(agent + " get proposition from " + message.getSender().getLocalName());
                try {
                    ACLMessage response = message.createReply();
                    response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    response.setContentObject(agent.getLocation());

                    agent.send(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                agent.getCitizenState().setValue(CitizenState.State.WAIT_FOR_GUEST);
                MyLog.log(agent + " waiting for guest");

                break;

            case ACLMessage.INFORM:
                long timeForGuest = Long.parseLong(message.getContent());
                MyLog.log(agent + " will be with guest for " + timeForGuest);
                try {
                    Thread.sleep(timeForGuest);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            case ACLMessage.CANCEL:
                MyLog.log(agent + " is ready to have guests");
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
