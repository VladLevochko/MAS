package ua.kpi.agents;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.CitizenState;
import ua.kpi.behaviors.GuestInstinct;
import ua.kpi.behaviors.HostBehaviour;
import jade.core.Agent;

public class Citizen extends Agent {
    private static long ACTIVITY_PERIOD = 24 * 60 * 60 * 1000 / 2 / Main.MODELLING_SPEED;

    private CitizenState state;
    private AgentLocation location;

    public Citizen(AgentLocation location) {
        this.state = new CitizenState();
        this.state.setValue(CitizenState.State.AT_HOME);
        this.location = location;
    }

    protected void setup() {
        MyLog.log("Setting up " + this);

        // TODO: refactor duplicate code
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("passenger");
        sd.setName(getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new HostBehaviour(this));
//        if (getLocalName().equals("citizen 0")) {
//            addBehaviour(new GuestInstinct(this, 1000));
//        } else {
            addBehaviour(new GuestInstinct(this, ACTIVITY_PERIOD));
//        }
    }

    protected void takeDown() {
        MyLog.log(this + " left the city");
    }

    public CitizenState getCitizenState() {
        return state;
    }

    public AgentLocation getLocation() {
        return this.location;
    }

    public String toString() {
        return getLocalName();
    }
}
