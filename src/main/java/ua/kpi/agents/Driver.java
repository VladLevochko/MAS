package ua.kpi.agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import ua.kpi.MyLog;
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
        return 100500;
    }

    public String toString() {
        return getLocalName();
    }
}
