package ua.kpi.behaviors;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import ua.kpi.MyLog;
import ua.kpi.agents.Driver;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.DriverState;
import ua.kpi.properties.TripInformation;

import java.io.IOException;

public class DriverBehaviour extends CyclicBehaviour {

    private Driver agent;
    private AID client;
    private long startWaiting;

    public DriverBehaviour(Driver agent) {
        this.agent = agent;
        this.startWaiting = 0;
    }

    @Override
    public void action() {
        ACLMessage message = agent.receive();
        if (message != null) {

            if (mustBeFree()) {
                agent.setDriverState(DriverState.FREE);
                startWaiting = 0;
            }

            switch (message.getPerformative()) {
                case ACLMessage.PROPOSE:
                    MyLog.log(agent + " received proposition to drive");
                    if (agent.getDriverState() == DriverState.FREE) {
                        ACLMessage response = message.createReply();
                        response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        try {
                            response.setContentObject(agent.getLocation());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        agent.send(response);
                        client = message.getSender();
                        startWaiting = System.currentTimeMillis();
                        agent.setDriverState(DriverState.BUSY);
                        MyLog.log(agent + " replied to " + message.getSender().getLocalName() + " that he is free");
                    }

                    break;
                case ACLMessage.CONFIRM:  // TODO: maybe wait for response from some specific passenger
                    MyLog.log(agent + " received confirmation from " + message.getSender().getLocalName());

                    if (message.getSender() != client) {
                        break;
                    }

                    try {
                        AgentLocation[] path = (AgentLocation[]) message.getContentObject();

                        TripInformation tripInformation = calculateTripInformation(path);
                        MyLog.log(agent + " calculated " + tripInformation);
                        ACLMessage response = message.createReply();
                        response.setContentObject(tripInformation);

                        agent.send(response);

                        Thread.sleep((long) tripInformation.getTotalTime());

                        agent.setLocation(path[1]);
                    } catch (UnreadableException | IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                case ACLMessage.CANCEL:
                    agent.setDriverState(DriverState.FREE);
                    client = null;
            }
        } else {
            block();
        }
    }

    private boolean mustBeFree() {
        return System.currentTimeMillis() - startWaiting > 2000;
    }

    private TripInformation calculateTripInformation(AgentLocation[] path) {
        AgentLocation driverLocation = agent.getLocation();
        AgentLocation passengerLocation = path[0];
        AgentLocation destination = path[1];
        double timeToPassenger = calculateTime(driverLocation, passengerLocation);
        double timeToDestination = calculateTime(passengerLocation, destination);

        return new TripInformation(timeToPassenger, timeToDestination);
    }

    private double calculateTime(AgentLocation from, AgentLocation to) {
        double distance = from.distanceTo(to);
        return distance / agent.getSpeed();
    }
}
