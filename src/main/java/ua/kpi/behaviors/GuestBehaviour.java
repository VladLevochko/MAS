package ua.kpi.behaviors;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.agents.TaxiService;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.CitizenState;
import ua.kpi.agents.Citizen;
import ua.kpi.properties.TripInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuestBehaviour extends Behaviour {
    private String CONVERSATION_ID = "visit_proposal";

    private Citizen agent;
    private MessageTemplate replyTemplate;
    private List<AID> agents;
    private List<AID> potentialHosts;
    private Map<AID, AgentLocation> locations;
    private boolean isDone;
    private AID host;
    private long timeToBack;

    public GuestBehaviour(Citizen agent) {
        this.agent = agent;
        this.agents = getAgents();
        potentialHosts = new ArrayList<>();
        locations = new HashMap<>();
    }

    @Override
    public void action() {
        CitizenState state = agent.getCitizenState();
        potentialHosts.clear();
        switch (state.getValue()) {
            case GUEST:
//                MyLog.log(agent + " sends propositions");
                String proposalReply = "propose" + System.currentTimeMillis();
                proposeVisit(agents, proposalReply);

                replyTemplate = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(CONVERSATION_ID),
                        MessageTemplate.MatchInReplyTo(proposalReply)
                );
                state.setValue(CitizenState.State.GUEST_WAITING_RESPONSES);
//                MyLog.log(agent + " start waiting for responses");

                break;
            case GUEST_WAITING_RESPONSES:
                for (int i = 0; i < agents.size(); i++) {
                    ACLMessage message = agent.blockingReceive(replyTemplate, 1000);

                    if (message == null) {
                        continue;
                    }
                    if (message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        AID sender = message.getSender();
                        potentialHosts.add(sender);
                        try {
                            locations.put(sender, (AgentLocation) message.getContentObject());
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (potentialHosts.size() == 0) {
                    agent.getCitizenState().setValue(CitizenState.State.AT_HOME);
                    break;
                }

                AID host = pickHost(potentialHosts);
                MyLog.log(agent + " is going to " + host.getLocalName());
                sendApproval(host);
                sendRejectsExcept(potentialHosts, host);
                this.host = host;

                boolean status = goToHost(host);
                if (!status) {
                    agent.getCitizenState().setValue(CitizenState.State.AT_HOME);
                    return;
                }

                agent.getCitizenState().setValue(CitizenState.State.OUT_OF_HOME);
                break;
            case OUT_OF_HOME:
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (System.currentTimeMillis() < timeToBack) {
                    break;
                }

                backFromHost(this.host);
                agent.getCitizenState().setValue(CitizenState.State.COMING_HOME);
                break;
            case COMING_HOME:
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (System.currentTimeMillis() < timeToBack) {
                    break;
                }

                agent.getCitizenState().setValue(CitizenState.State.AT_HOME);
                isDone = true;
        }
    }

    private List<AID> getAgents() {
        List<AID> agents = new ArrayList<>();
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("passenger");
        template.addServices(serviceDescription);

        DFAgentDescription[] passengers = null;
        try {
            passengers = DFService.searchUntilFound(agent, agent.getDefaultDF(), template, null, 60000);
        } catch (FIPAException e){
            e.printStackTrace();
        }

        if (passengers == null){
            return agents;
        }

        for (DFAgentDescription passenger : passengers) {
            if (agent.getAID().equals(passenger.getName())) {
                continue;
            }

            agents.add(passenger.getName());
        }

        return agents;
    }

    private void proposeVisit(List<AID> agents, String proposalReply) {
        ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);

        for (AID passenger : agents) {
            proposal.addReceiver(passenger);
        }

        proposal.setConversationId(CONVERSATION_ID);
        proposal.setReplyWith(proposalReply);

        agent.send(proposal);
    }

    private AID pickHost(List<AID> potentialHosts) {
        int hostIndex = (int) (Math.random() * (potentialHosts.size() - 1));
        return potentialHosts.get(hostIndex);
    }

    private void sendApproval(AID host) {
        ACLMessage message = new ACLMessage(ACLMessage.CONFIRM);
        message.addReceiver(host);
        message.setConversationId(CONVERSATION_ID);

        agent.send(message);
    }

    private void sendRejectsExcept(List<AID> agents, AID host) {
        for (AID agent : agents) {
            if (agent == host) {
                continue;
            }

            ACLMessage message = new ACLMessage(ACLMessage.CANCEL);
            message.addReceiver(agent);
            message.setConversationId(CONVERSATION_ID);

            this.agent.send(message);
        }
    }

    private boolean goToHost(AID host) {
        TaxiService taxi = TaxiService.getInstance();
//        MyLog.log(agent + " is requesting taxi");
        TripInformation tripInformation = taxi.requestDriver(agent, agent.getLocation(), locations.get(host));
        if (tripInformation == null) {
            isDone = true;
            return false;
        }

        double stayTime = Math.random() * 3 * 60 * 60;
        long totalTime = (long) (tripInformation.getTimeToPassenger()
                + tripInformation.getTimeToDestination()
                + stayTime);

        notifyHostAboutTime(host, totalTime);

        timeToBack = System.currentTimeMillis() + totalTime * 1000 / Main.MODELLING_SPEED;

        agent.updateMoney(-tripInformation.getCost());

        return true;
    }

    private void notifyHostAboutTime(AID host, long time) {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(host);
        message.setContent(Long.toString(time));

        agent.send(message);
    }

    private void backFromHost(AID host) {
        TaxiService taxi = TaxiService.getInstance();
        TripInformation tripInformation;
        while (true) {
            tripInformation = taxi.requestDriver(agent, locations.get(host), agent.getLocation());
            if (tripInformation == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        long totalTime = (long) tripInformation.getTimeToPassenger()
                + (long) tripInformation.getTimeToDestination();
//        try {
//            Thread.sleep(totalTime * 1000 / Main.MODELLING_SPEED);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        timeToBack = System.currentTimeMillis() + totalTime * 1000 / Main.MODELLING_SPEED;
    }

    @Override
    public boolean done() {
        return isDone;
    }
}
