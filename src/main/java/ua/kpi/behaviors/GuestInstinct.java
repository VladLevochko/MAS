package ua.kpi.behaviors;

import jade.core.behaviours.TickerBehaviour;
import ua.kpi.properties.CitizenState;
import ua.kpi.agents.Citizen;

public class GuestInstinct extends TickerBehaviour {
    private Citizen agent;
    private long period;

    public GuestInstinct(Citizen agent, long period) {
        super(agent, period);
        this.agent = agent;
        this.period = period;
    }

    @Override
    protected void onTick() {
        CitizenState state = agent.getCitizenState();
        if (state.getValue() == CitizenState.State.GUEST) {
            return;
        }
        state.setGuest();
        agent.addBehaviour(new GuestBehaviour(agent));

        long nextGuestingIntention = (long) (Math.random() * this.period);
        reset(nextGuestingIntention);
    }
}
