package ua.kpi.behaviors;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import ua.kpi.Main;
import ua.kpi.agents.Citizen;

public class IncomeBehaviour extends TickerBehaviour {
    private static long PERIOD = 24 * 60 * 60 * 1000 / Main.MODELLING_SPEED;

    private Citizen agent;

    public IncomeBehaviour(Citizen agent) {
        super(agent, PERIOD);
        this.agent = agent;
    }

    @Override
    protected void onTick() {
        agent.updateMoney(Main.MODELLING_PARAMS.citizenIncome);
    }
}
