package ua.kpi.agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.behaviors.DriverBehaviour;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.DriverState;

public class Driver extends Agent {
    private static int driversNumber = 0;

    private final double SPEED = 16.67; // m/s

    private DriverState state;
    private AgentLocation location;

    public Driver() {
        state = DriverState.FREE;
        location = getFairLocation();
    }

    private AgentLocation getFairLocation() {
        int width = Main.CITY_TYPE.width;
        int height = Main.CITY_TYPE.height;

        int x = width / 2;
        int y = height / 2;
//        int quarter = driversNumber++ % 4;
//        int x = width / 4 + (quarter % 2) * width / 2;
//        int y = height / 4 + (quarter / 2) * height / 2;

        return new AgentLocation(x, y);
    }

    protected void setup() {
        MyLog.log("Setting up new driver");

        addBehaviour(new DriverBehaviour(this));

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("taxi");
        sd.setName(getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public void takeDown() {
        MyLog.log(this + " went to rest");
    }

    public DriverState getDriverState() {
        return state;
    }

    public void setDriverState(DriverState state) {
        this.state = state;
    }

    public AgentLocation getLocation() {
        return location;
    }

    public void setLocation(AgentLocation location) {
        this.location = location;
    }

    public double getSpeed() {
        return SPEED;
    }

    public String toString() {
        return getLocalName();
    }

    public String description() {
        return String.format("%s %s %s", getLocalName(), location, state);
    }
}
