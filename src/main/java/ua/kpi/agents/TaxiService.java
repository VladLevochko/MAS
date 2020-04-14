package ua.kpi.agents;

import jade.core.AID;
import jade.core.Agent;
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
import ua.kpi.behaviors.TaxiServiceBehaviour;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.TripInformation;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TaxiService extends Agent {
    private final long WAIT_DRIVER_RESPONSE_TIME = 1000;
    private final float PRICE = 2.5f;

    private static TaxiService instance;

    private List<Double> waitingTimes;
    private List<Integer> requestingTries;
    private Map<String, Integer> trips;
    private ReentrantReadWriteLock lock;
    private BiConsumer<Agent, String> newDriverCallback;
    private Consumer<Double> balanceCheckCallback;

    private double balance;
    private double cost;
    private double coefficient;

    private TaxiService() {
        waitingTimes = new ArrayList<>();
        requestingTries = new ArrayList<>();
        trips = new HashMap<>();
        lock = new ReentrantReadWriteLock();

        this.balance = 150000;
        this.cost = 2.5 / 1000;
        this.coefficient = 0.2;
    }

    public void setNewDriverCallback(BiConsumer<Agent, String> callback) {
        this.newDriverCallback = callback;
    }

    public void addBalanceCheckCallback(Consumer<Double> callback) {
        balanceCheckCallback = callback;
    }

    public static TaxiService getInstance() {
        if (instance == null) {
            instance = new TaxiService();
        }

        return instance;
    }

    protected void setup() {
        MyLog.log("setting up new TaxiService");

        TaxiServiceBehaviour behaviour = new TaxiServiceBehaviour(this, newDriverCallback);
        behaviour.setBalanceCheckCallback(balanceCheckCallback);

        addBehaviour(behaviour);
    }

    protected void takeDown() {
        MyLog.log("deleting TaxiService");
        instance = null;
    }

    public void updateBalance(double value) {
        balance += value;
    }

    public void setCoefficient(double value) {
        this.coefficient = value;
    }

    public double getBalance() {
        return this.balance;
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
        Set<AID> drivers = getDrivers();

        Map.Entry<AID, AgentLocation> driver = null;
        int tries = 0;

        double tic = System.currentTimeMillis();
        while (driver == null) {
            driver = findDriver(passenger, from, to, drivers);
            tries++;

        }
        double toc = System.currentTimeMillis();

        TripInformation tripInformation = calculateTripInformation(driver.getValue(), from, to);
        double price = calculatePrice(tripInformation);

        double waitingTime = (toc - tic) / 1000 * Main.MODELLING_SPEED + tripInformation.getTimeToPassenger();

//        if (passenger.satisfiedWithTrip(tripInformation.getTimeToPassenger(), price)) {
        if (Main.MODELLING_PARAMS.dissatisfaction.apply(waitingTime, price) <= 1) {
            drivers.remove(driver.getKey());
            sendCancellations(passenger, drivers);
            sendConfirmation(driver.getKey(), passenger, tripInformation);
        } else {
            sendCancellations(passenger, drivers);
            return null;
        }

        receivePayment(tripInformation);
        tripInformation.setCost(price);

        recordWaitingTime(waitingTime, tries);

        String driverName = driver.getKey().getLocalName();
        lock.writeLock().lock();
        trips.put(driverName, trips.getOrDefault(driverName, 0) + 1);
        lock.writeLock().unlock();

        return tripInformation;
    }

    public Set<AID> getDrivers() {
        Set<AID> drivers = new HashSet<>();

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

    private Map.Entry<AID, AgentLocation> findDriver(Citizen passenger, AgentLocation from, AgentLocation to, Set<AID> drivers) {
        Map<AID, AgentLocation> driversLocations = getDriversLocations(drivers, passenger);
        Map.Entry<AID, AgentLocation> closestDriver = findClosestDriver(driversLocations, from);
        if (closestDriver == null) {
            return null;
        }
        MyLog.log("chose driver " + closestDriver.getKey().getLocalName());

        return closestDriver;
    }

    private Map<AID, AgentLocation> getDriversLocations(Set<AID> drivers, Agent requester) {
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

    private Map.Entry<AID, AgentLocation> findClosestDriver(Map<AID, AgentLocation> driversLocations, AgentLocation targetLocation) {
        double smallestDistance = Double.MAX_VALUE;
        Map.Entry<AID, AgentLocation> closestDriver = null;

        for (Map.Entry<AID, AgentLocation> driver : driversLocations.entrySet()) {
            AgentLocation driverLocation = driver.getValue();
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

    private void sendConfirmation(AID driver, Citizen passenger, TripInformation tripInformation) {
        ACLMessage confirmation = new ACLMessage(ACLMessage.CONFIRM);
        confirmation.addReceiver(driver);
        confirmation.setReplyWith("trip_confirmation" + System.currentTimeMillis());

        try {
            confirmation.setContentObject(tripInformation);
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
    }

    private TripInformation calculateTripInformation(AgentLocation driverLocation, AgentLocation passengerLocation, AgentLocation destination) {
        double timeToPassenger = calculateTime(driverLocation, passengerLocation);
        double timeToDestination = calculateTime(passengerLocation, destination);

        TripInformation tripInformation = new TripInformation(timeToPassenger, timeToDestination);
        tripInformation.setDistanceToPassenger(driverLocation.distanceTo(passengerLocation));
        tripInformation.setDistanceToDestination(passengerLocation.distanceTo(destination));

        tripInformation.setDestination(destination);

        return tripInformation;
    }

    private double calculateTime(AgentLocation from, AgentLocation to) {
        double distance = from.distanceTo(to);
        return distance / Driver.SPEED;
    }

    private double calculatePrice(TripInformation tripInformation) {
        return tripInformation.getTotalDistance() * getPriceOfMeter();
    }

    private double getPriceOfMeter() {
        return this.cost * (1 + this.coefficient);
    }

    private void receivePayment(TripInformation tripInformation) {
        this.balance += tripInformation.getTotalDistance() * this.coefficient * this.cost;
    }

    private void recordWaitingTime(double waitingTime, int tries) {
        lock.writeLock().lock();
        waitingTimes.add(waitingTime);
        requestingTries.add(tries);
        lock.writeLock().unlock();
    }

}
