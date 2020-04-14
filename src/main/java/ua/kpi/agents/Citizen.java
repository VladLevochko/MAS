package ua.kpi.agents;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.behaviors.IncomeBehaviour;
import ua.kpi.properties.AgentLocation;
import ua.kpi.properties.CitizenState;
import ua.kpi.behaviors.GuestInstinct;
import ua.kpi.behaviors.HostBehaviour;
import jade.core.Agent;

public class Citizen extends Agent {
    private CitizenState state;
    private AgentLocation location;
    private double money;

    public Citizen(AgentLocation location) {
        this.state = new CitizenState();
        this.state.setValue(CitizenState.State.AT_HOME);
        this.location = location;
        this.money = 100;
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
            addBehaviour(new HostBehaviour(this));
            addBehaviour(new GuestInstinct(this));
            addBehaviour(new IncomeBehaviour(this));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
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

    public void updateMoney(double value) {
        this.money += value;
    }

    public double getMoney() {
        return this.money;
    }

    public boolean satisfiedWithTrip(double waitingTime, double price) {
        return price <= this.money
                && Main.MODELLING_PARAMS.dissatisfaction.apply(waitingTime, price) <= 1;
    }
}
