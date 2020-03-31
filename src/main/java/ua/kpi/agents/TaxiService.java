package ua.kpi.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import ua.kpi.MyLog;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.TripInformation;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TaxiService extends Agent {
    private final long WAIT_DRIVER_RESPONSE_TIME = 1000;

    private static TaxiService instance;

    private List<Long> waitingTimes;
    private ReentrantReadWriteLock lock;

    private TaxiService() {
        waitingTimes = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
        // TODO: add behaviour for adding new taxi drivers each day
    }

    public static TaxiService getInstance() {
        if (instance == null) {
            instance = new TaxiService();
        }

        return instance;
    }

    public void setup() {

    }

    public TripInformation requestDriver(Citizen passenger, AgentLocation from, AgentLocation to) {
        long tic = System.currentTimeMillis();

        AID driver = null;
        while (driver == null) {
            driver = findDriver(passenger, from, to);
        }

        TripInformation tripInformation = tripWithDriver(driver, passenger, from, to);

        long toc = System.currentTimeMillis();
        recordWaitingTime(toc - tic);

        return tripInformation;
    }

    private AID findDriver(Citizen passenger, AgentLocation from, AgentLocation to) {
        MyLog.log("looking for drivers");
        List<AID> drivers = getDrivers();
        MyLog.log(String.format("found %d driver", drivers.size()));
        if (drivers.size() == 0) {
            return null;
        }

        Map<AID, AgentLocation> driversLocations = getDriversLocations(drivers, passenger);
        AID closestDriver = findClosestDriver(driversLocations, from);
        MyLog.log("chose driver " + closestDriver.getLocalName());

        Set<AID> driversToCancel = driversLocations.keySet();
        driversLocations.remove(closestDriver);
        sendCancellations(passenger, driversToCancel);



        return closestDriver;
    }

    private List<AID> getDrivers() {
        List<AID> drivers = new ArrayList<>();

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("taxi");
        template.addServices(serviceDescription);

        try {
            DFAgentDescription[] taxiDrivers = DFService.search(this, template);
            for (DFAgentDescription agentDescription : taxiDrivers) {
                drivers.add(agentDescription.getName());
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return drivers;
    }


    private Map<AID, AgentLocation> getDriversLocations(List<AID> drivers, Agent requester) {
        Map<AID, AgentLocation> locations = new HashMap<>();

        ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
        for (AID driver : drivers) {
            message.addReceiver(driver);
        }
        requester.send(message);

        MessageTemplate responseTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
        for (int i = 0; i < drivers.size(); i++) {
            ACLMessage response = requester.blockingReceive(responseTemplate, WAIT_DRIVER_RESPONSE_TIME);
            if (response == null) {
                continue;
            }

            try {
                AgentLocation location = (AgentLocation) response.getContentObject();
                locations.put(response.getSender(), location);
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
        }

        return locations;
    }

    private AID findClosestDriver(Map<AID, AgentLocation> driversLocations, AgentLocation targetLocation) {
        double smallestDistance = Double.MAX_VALUE;
        AID closestDriver = null;

        for (AID driver : driversLocations.keySet()) {
            AgentLocation driverLocation = driversLocations.get(driver);
            double distance = driverLocation.distanceTo(targetLocation);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                closestDriver = driver;
            }
        }

        return closestDriver;
    }

    private void sendCancellations(Citizen passenger, Set<AID> drivers) {
        ACLMessage cancellation = new ACLMessage(ACLMessage.CANCEL);
        for (AID driver : drivers) {
            cancellation.addReceiver(driver);
        }

        passenger.send(cancellation);
    }

    private boolean sendApproval(Citizen passenger, AID driver) {
        ACLMessage message = new ACLMessage(ACLMessage.AGREE);
        message.addReceiver(driver);
        message.setReplyWith("approve" + System.currentTimeMillis());
        passenger.send(message);

        MessageTemplate responseTemplate = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                MessageTemplate.MatchInReplyTo(message.getReplyWith())
        );
        ACLMessage response = passenger.blockingReceive(responseTemplate, 1000);

        return response == null;
    }

    private TripInformation tripWithDriver(AID driver, Citizen passenger, AgentLocation from, AgentLocation to) {
        MessageTemplate responseTemplate = sendConfirmation(driver, passenger, from, to);
        TripInformation tripInformation = receiveTripInformation(passenger, responseTemplate);

        return tripInformation;
    }

    private MessageTemplate sendConfirmation(AID driver, Citizen passenger, AgentLocation from, AgentLocation to) {
        ACLMessage confirmation = new ACLMessage(ACLMessage.CONFIRM);
        confirmation.addReceiver(driver);
        confirmation.setReplyWith("trip_confirmation" + System.currentTimeMillis());

        AgentLocation[] path = new AgentLocation[] {from, to};
        try {
            confirmation.setContentObject(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (confirmation.getContentObject() == null) {
                MyLog.log(passenger + " happened error during setting locations for trip!!!");
            }
        } catch (UnreadableException e) {
            MyLog.log(passenger  + " happened error during setting locations for trip!!!");
            e.printStackTrace();
        }

        passenger.send(confirmation);

        MessageTemplate responseTemplate = MessageTemplate.MatchInReplyTo(confirmation.getReplyWith());

        return responseTemplate;
    }

    private TripInformation receiveTripInformation(Citizen passenger, MessageTemplate responseTemplate) {
        ACLMessage response = passenger.blockingReceive(responseTemplate);

        TripInformation tripInformation = null;
        try {
            tripInformation = (TripInformation) response.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }

        return tripInformation;
    }

    private void recordWaitingTime(long waitingTime) {
        lock.writeLock().lock();
        waitingTimes.add(waitingTime);
        lock.writeLock().unlock();
    }
}
