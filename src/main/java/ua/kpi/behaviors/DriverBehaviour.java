package ua.kpi.behaviors;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.agents.Driver;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.DriverState;
import ua.kpi.properties.TripInformation;

import java.io.IOException;

public class DriverBehaviour extends CyclicBehaviour {

    private Driver agent;
    private AID client;
    private long endTripAt;

    public DriverBehaviour(Driver agent) {
        this.agent = agent;
    }

    @Override
    public void action() {
        if (agent.getDriverState() == DriverState.DRIVING && endTripAt <= System.currentTimeMillis()) {
            agent.setDriverState(DriverState.FREE);
        }

        ACLMessage message = agent.receive();

        if (message != null) {
            switch (message.getPerformative()) {
                case ACLMessage.PROPOSE:
//                    MyLog.log(agent + " received proposition to drive");
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
                        agent.setDriverState(DriverState.WAITING_FOR_PASSENGER);
//                        MyLog.log(agent + " replied to " + message.getSender().getLocalName() + " that he is free");
                    } else {
                        ACLMessage response = message.createReply();
                        response.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        agent.send(response);
                    }

                    break;
                case ACLMessage.CONFIRM:
                    MyLog.log(agent + " received confirmation from " + message.getSender().getLocalName());

                    if (!message.getSender().equals(client)) {
                        break;
                    }

                    try {
                        AgentLocation[] path = (AgentLocation[]) message.getContentObject();

                        TripInformation tripInformation = calculateTripInformation(path);
                        MyLog.log(agent + " calculated " + tripInformation);
                        ACLMessage response = message.createReply();
                        response.setContentObject(tripInformation);

                        agent.send(response);

                        endTripAt = System.currentTimeMillis() + (long) tripInformation.getTotalTime() * 1000 / Main.MODELLING_SPEED;
                        agent.setDriverState(DriverState.DRIVING);

                        agent.setLocation(path[1]);
                    } catch (UnreadableException | IOException e) {
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

    private TripInformation calculateTripInformation(AgentLocation[] path) {
        AgentLocation driverLocation = agent.getLocation();
        AgentLocation passengerLocation = path[0];
        AgentLocation destination = path[1];
        double timeToPassenger = calculateTime(driverLocation, passengerLocation);
        double timeToDestination = calculateTime(passengerLocation, destination);

        TripInformation tripInformation = new TripInformation(timeToPassenger, timeToDestination);
        tripInformation.setDistanceToPassenger(driverLocation.distanceTo(passengerLocation));
        tripInformation.setDistanceToDestination(passengerLocation.distanceTo(destination));

        return tripInformation;
    }

    private double calculateTime(AgentLocation from, AgentLocation to) {
        double distance = from.distanceTo(to);
        return distance / agent.getSpeed();
    }
}
