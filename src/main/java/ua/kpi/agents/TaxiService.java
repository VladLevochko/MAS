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
import ua.kpi.behaviors.TaxiServiceBehaviour;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.TripInformation;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class TaxiService extends Agent {
    private final long WAIT_DRIVER_RESPONSE_TIME = 1000;

    private static TaxiService instance;

    private List<Double> waitingTimes;
    private List<Integer> requestingTries;
    private Map<String, Integer> trips;
    private ReentrantReadWriteLock lock;
    private BiConsumer<Agent, String> newDriverCallback;

    private TaxiService() {
        waitingTimes = new ArrayList<>();
        requestingTries = new ArrayList<>();
        trips = new HashMap<>();
        lock = new ReentrantReadWriteLock();
    }

    public void setNewDriverCallback(BiConsumer<Agent, String> callback) {
        this.newDriverCallback = callback;
    }

    public static TaxiService getInstance() {
        if (instance == null) {
            instance = new TaxiService();
        }

        return instance;
    }

    public void setup() {
        addBehaviour(new TaxiServiceBehaviour(this, newDriverCallback));
    }

    public ReentrantReadWriteLock getStorageLock() {
        return this.lock;
    }

    public List<Double> getWaitingTimes() {
        return this.waitingTimes;
    }

    public List<Integer> getRequestingTries() {
        return this.requestingTries;
    }

    public Map<String, Integer> getTrips() {
        return this.trips;
    }

    public TripInformation requestDriver(Citizen passenger, AgentLocation from, AgentLocation to) {
        List<AID> drivers = getDrivers();

        AID driver = null;
        int tries = 0;

        double tic = System.currentTimeMillis();
        while (driver == null) {
            driver = findDriver(passenger, from, to, drivers);
            tries++;
        }
        double toc = System.currentTimeMillis();

        recordWaitingTime(toc - tic, tries);

        TripInformation tripInformation = tripWithDriver(driver, passenger, from, to);

        String driverName = driver.getLocalName();
        lock.writeLock().lock();
        trips.put(driverName, trips.getOrDefault(driverName, 0) + 1);
        lock.writeLock().unlock();

        return tripInformation;
    }

    private AID findDriver(Citizen passenger, AgentLocation from, AgentLocation to, List<AID> drivers) {
        Map<AID, AgentLocation> driversLocations = getDriversLocations(drivers, passenger);
        AID closestDriver = findClosestDriver(driversLocations, from);
        if (closestDriver == null) {
            return null;
        }
        MyLog.log("chose driver " + closestDriver.getLocalName());

        Set<AID> driversToCancel = new HashSet<>(drivers);
        driversToCancel.remove(closestDriver);
        sendCancellations(passenger, driversToCancel);

        return closestDriver;
    }

    public List<AID> getDrivers() {
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
//            e.printStackTrace();
        }

        return drivers;
    }


    private Map<AID, AgentLocation> getDriversLocations(List<AID> drivers, Agent requester) {
        Map<AID, AgentLocation> locations = new HashMap<>();

        ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
        for (AID driver : drivers) {
            message.addReceiver(driver);
        }
        message.setReplyWith("position_request" + requester.getLocalName() + System.currentTimeMillis());
        requester.send(message);

        MessageTemplate responseTemplate = MessageTemplate.MatchInReplyTo(message.getReplyWith());
        for (int i = 0; i < drivers.size(); i++) {
            ACLMessage response = requester.blockingReceive(responseTemplate, WAIT_DRIVER_RESPONSE_TIME);
            if (response == null || response.getPerformative() != ACLMessage.ACCEPT_PROPOSAL) {
                continue;
            }

            try {
                AgentLocation location = (AgentLocation) response.getContentObject();
                locations.put(response.getSender(), location);
//                MyLog.log(String.format("%s now at %s", response.getSender().getLocalName(), location));
            } catch (UnreadableException e) {
                MyLog.log("error receiving drivers location");
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

    private void recordWaitingTime(double waitingTime, int tries) {
        lock.writeLock().lock();
        waitingTimes.add(waitingTime);
        requestingTries.add(tries);
        lock.writeLock().unlock();
    }
}
