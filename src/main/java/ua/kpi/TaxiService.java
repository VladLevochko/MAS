package ua.kpi;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import ua.kpi.agents.Citizen;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.TripInformation;

import java.io.IOException;
import java.util.*;

public class TaxiService extends Agent {

    private static TaxiService instance;

    private List<AID> drivers;

    private TaxiService() {
        drivers = getDrivers();
    }

    public static TaxiService getInstance() {
        if (instance == null) {
            instance = new TaxiService();
        }

        return instance;
    }

    public void setup() {

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

    public TripInformation requestDriver(Citizen passenger, AgentLocation from, AgentLocation to) {

        Map<AID, AgentLocation> driversLocations = getDriversLocations(drivers);
        AID closestDriver = findClosestDriver(driversLocations, from);

        Set<AID> driversToCancel = driversLocations.keySet();
        driversLocations.remove(closestDriver);
        sendCancellations(passenger, driversToCancel);

        TripInformation tripInformation = tripWithDriver(closestDriver, passenger, from, to);

        return tripInformation;
    }

    private Map<AID, AgentLocation> getDriversLocations(List<AID> drivers) {
        Map<AID, AgentLocation> locations = new HashMap<>();

        // TODO: implement logic of locations retrieval

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

        passenger.send(confirmation);

        MessageTemplate responseTemplate = MessageTemplate.MatchReplyWith(confirmation.getReplyWith());

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
}
