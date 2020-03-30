package ua.kpi.behaviors;

import jade.core.behaviours.TickerBehaviour;
import ua.kpi.MyLog;
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
        if (!state.isAtHome() || state.getValue() == CitizenState.State.WAIT_FOR_GUEST) {
            return;
        }
        MyLog.log(agent.toString() + " need to visit somebody");

        state.setGuest();
        agent.addBehaviour(new GuestBehaviour(agent));

        long nextGuestingIntention = (long) (Math.random() * this.period);
        reset(nextGuestingIntention);
    }
}
