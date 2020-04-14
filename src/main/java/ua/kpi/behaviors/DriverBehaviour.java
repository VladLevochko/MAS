package ua.kpi.behaviors;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.agents.Driver;
import ua.kpi.properties.DriverState;
import ua.kpi.properties.TripInformation;

import java.io.IOException;

public class DriverBehaviour extends CyclicBehaviour {

    private Driver agent;
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
                            MyLog.log(agent + "error sending location");
                            e.printStackTrace();
                        }

                        agent.send(response);
                        agent.setDriverState(DriverState.WAITING_FOR_PASSENGER);
//                        MyLog.log(agent + " replied to " + message.getSender().getLocalName() + " that he is free");
                    } else {
                        ACLMessage response = message.createReply();
                        response.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        agent.send(response);
                    }

                    break;
                case ACLMessage.CONFIRM:
                    if (agent.getDriverState() != DriverState.WAITING_FOR_PASSENGER) {
                        break;
                    }

                    MyLog.log(agent + " received confirmation from " + message.getSender().getLocalName());

                    try {
                        TripInformation tripInformation = (TripInformation) message.getContentObject();
                        MyLog.log(agent + " is having a ride " + tripInformation);

                        endTripAt = System.currentTimeMillis() + (long) tripInformation.getTotalTime() * 1000 / Main.MODELLING_SPEED;
                        agent.setDriverState(DriverState.DRIVING);

                        agent.setLocation(tripInformation.getDestination());
                    } catch (UnreadableException e) {
                        agent.setDriverState(DriverState.FREE);
                        e.printStackTrace();
                    }
                case ACLMessage.CANCEL:
                    if (agent.getDriverState() == DriverState.WAITING_FOR_PASSENGER) {
                        agent.setDriverState(DriverState.FREE);
                    }
            }
        } else {
            block();
        }
    }
}