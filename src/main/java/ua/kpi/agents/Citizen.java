package ua.kpi.agents;

import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.CitizenState;
import ua.kpi.behaviors.GuestInstinct;
import ua.kpi.behaviors.HostBehaviour;
import jade.core.Agent;

public class Citizen extends Agent {
    private static long ACTIVITY_PERIOD = 24 * 60 * 60 * 1000 / 2;

    private CitizenState state;
    private AgentLocation location;

    public Citizen(AgentLocation location) {
        this.state.setValue(CitizenState.State.AT_HOME);
        this.location = location;
    }

    protected void setup() {
        state = new CitizenState();
        addBehaviour(new HostBehaviour());
        addBehaviour(new GuestInstinct(this, ACTIVITY_PERIOD));
    }

    protected void takeDown() {

    }

    public CitizenState getCitizenState() {
        return state;
    }

    public AgentLocation getLocation() {
        return this.location;
    }
}
