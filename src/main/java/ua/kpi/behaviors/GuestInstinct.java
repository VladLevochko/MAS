package ua.kpi.behaviors;

import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import ua.kpi.Main;
import ua.kpi.properties.CitizenState;
import ua.kpi.agents.Citizen;

public class GuestInstinct extends TickerBehaviour {
    private static long ACTIVITY_PERIOD = 24 * 60 * 60 * 1000 / 2 / Main.MODELLING_SPEED;

    private Citizen agent;
    private long period;

    public GuestInstinct(Citizen agent) {
        super(agent, (long) (Math.random() * ACTIVITY_PERIOD));
        this.agent = agent;
        this.period = ACTIVITY_PERIOD;
    }

    @Override
    protected void onTick() {
        CitizenState state = agent.getCitizenState();
        long nextGuestingIntention;
        if (state.getValue() == CitizenState.State.AT_HOME) {
            state.setGuest();
            agent.addBehaviour(new GuestBehaviour(agent));
        }
        nextGuestingIntention = (long) (Math.random() * this.period) + 1;

        reset(nextGuestingIntention);
    }
}
