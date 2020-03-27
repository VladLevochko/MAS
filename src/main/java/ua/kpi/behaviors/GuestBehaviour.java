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
import ua.kpi.TaxiService;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.CitizenState;
import ua.kpi.agents.Citizen;
import ua.kpi.properties.TripInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuestBehaviour extends Behaviour {
    private String CONVERSATION_ID = "visit_proposal";  // TODO: check correctness

    private Citizen agent;
    private MessageTemplate replyTemplate;
    private List<AID> agents;
    private int responses;
    private List<AID> potentialHosts;
    private Map<AID, AgentLocation> locations;

    public GuestBehaviour() {
        agent = (Citizen) myAgent;
        agents = getAgents();
        potentialHosts = new ArrayList<>();
        locations = new HashMap<>();
    }

    @Override
    public void action() {
        CitizenState state = agent.getCitizenState();
        switch (state.getValue()) {
            case GUEST:
                String proposalReply = "propose" + System.currentTimeMillis();
                proposeVisit(agents, proposalReply);

                replyTemplate = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(CONVERSATION_ID),
                        MessageTemplate.MatchReplyWith(proposalReply)
                );
                state.setValue(CitizenState.State.GUEST_WAITING_RESPONSES);

                break;
            case GUEST_WAITING_RESPONSES:
                ACLMessage message = agent.receive(replyTemplate);
                if (message != null) {
                    if (message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        AID sender = message.getSender();
                        potentialHosts.add(sender);
                        try {
                            locations.put(sender, (AgentLocation) message.getContentObject());
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                    responses++;

                    if (responses == agents.size()) {
                        agent.getCitizenState().setValue(CitizenState.State.GUEST_TRAVELING);
                    }
                } else {
                    block();
                }
                break;
            case GUEST_TRAVELING:
                AID host = pickHost(potentialHosts);
                sendApproval(host);
                sendRejectsExcept(potentialHosts, host);


                goToHost(host);
                backFromHost(host);
                agent.getCitizenState().setValue(CitizenState.State.AT_HOME);

                break;
        }
    }

    private List<AID> getAgents() {
        List<AID> agents = new ArrayList<>();
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("passengerVisit");

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

    private void goToHost(AID host) {
        TaxiService taxi = TaxiService.getInstance();
        TripInformation tripInformation = taxi.requestDriver(agent, agent.getLocation(), locations.get(host));

        double stayTime = Math.random() * 3 * 60 * 60 * 1000;
        double totalTime = tripInformation.getTimeToPassenger()
                + tripInformation.getTimeToDestination()
                + stayTime;

        notifyHostAboutTime(host, (long) totalTime);
    }

    private void notifyHostAboutTime(AID host, long time) {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(host);
        message.setContent(Long.toString(time));

        agent.send(message);
    }

    private void backFromHost(AID host) {
        TaxiService taxi = TaxiService.getInstance();
        TripInformation tripInformation = taxi.requestDriver(agent, locations.get(host), agent.getLocation());
        long totalTime = (long) tripInformation.getTimeToPassenger()
                + (long) tripInformation.getTimeToDestination();
        try {
            Thread.sleep(totalTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean done() {
        return false;
    }
}
