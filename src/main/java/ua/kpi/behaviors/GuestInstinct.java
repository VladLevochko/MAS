package ua.kpi.behaviors;

import jade.core.behaviours.TickerBehaviour;
import ua.kpi.properties.CitizenState;
import ua.kpi.agents.Citizen;

public class GuestInstinct extends TickerBehaviour {
    private Citizen agent;
    private long period;

    public GuestInstinct(Citizen agent, long period) {
        super(agent, (long) (Math.random() * period));
        this.agent = agent;
        this.period = period;
    }

    @Override
    protected void onTick() {
        CitizenState state = agent.getCitizenState();
        long nextGuestingIntention;
        if (state.getValue() == CitizenState.State.AT_HOME) {
            state.setGuest();
            agent.addBehaviour(new GuestBehaviour(agent));
            nextGuestingIntention = (long) (Math.random() * this.period);
        } else {
            nextGuestingIntention = (long) (Math.random() * this.period / 8);
        }

        reset(nextGuestingIntention);
    }
}
