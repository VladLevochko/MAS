package ua.kpi.behaviors;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import ua.kpi.agents.Driver;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.DriverState;
import ua.kpi.properties.TripInformation;

import java.io.IOException;

public class DriverBehaviour extends CyclicBehaviour {

    private Driver agent;

    public DriverBehaviour() {
        agent = (Driver) myAgent;
    }

    @Override
    public void action() {
        ACLMessage message = agent.receive();
        if (message != null) {
            switch (message.getPerformative()) {
                case ACLMessage.PROPOSE:
                    if (agent.getDriverState() == DriverState.FREE) {
                        ACLMessage response = message.createReply();
                        response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        try {
                            response.setContentObject(agent.getLocation());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        agent.send(response);
                        agent.setDriverState(DriverState.WAITING_);
                    }

                    break;
                case ACLMessage.CONFIRM:  // TODO: maybe wait for response from some specific passenger
                    try {
                        AgentLocation[] path = (AgentLocation[]) message.getContentObject();
                        TripInformation tripInformation = calculateTripInformation(path);
                        ACLMessage response = message.createReply();
                        response.setContentObject(tripInformation);

                        agent.send(response);

                        Thread.sleep((long) tripInformation.getTotalTime());
                    } catch (UnreadableException | IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    agent.setDriverState(DriverState.FREE);
                case ACLMessage.CANCEL:
                    agent.setDriverState(DriverState.FREE);
            }
        } else {
            block();
        }
    }


    private TripInformation calculateTripInformation(AgentLocation[] path) {
        double timeToPassenger = calculateTime(agent.getLocation(), path[0]);
        double timeToDestination = calculateTime(path[0], path[1]);

        return new TripInformation(timeToPassenger, timeToDestination);
    }

    private double calculateTime(AgentLocation from, AgentLocation to) {
        double distance = from.distanceTo(to);
        return distance / agent.getSpeed();
    }
}
