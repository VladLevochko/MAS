package ua.kpi.agents;

import jade.core.Agent;
import ua.kpi.behaviors.DriverBehaviour;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.DriverState;

public class Driver extends Agent {

    private DriverState state;
    private AgentLocation location;

    public Driver() {
        state = DriverState.FREE;
        location = new AgentLocation();
    }

    protected void setup() {
        addBehaviour(new DriverBehaviour());
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
        return 100500;
    }
}
