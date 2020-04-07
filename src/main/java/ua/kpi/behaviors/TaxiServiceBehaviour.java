package ua.kpi.behaviors;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.agents.Driver;
import ua.kpi.agents.TaxiService;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class TaxiServiceBehaviour extends TickerBehaviour {
    private static long PERIOD = 24 * 60 * 60 * 1000 / Main.MODELLING_SPEED;
    private long WAITING_THRESHOLD = 60 * 60 * 1000 / Main.MODELLING_SPEED;

    private TaxiService service;
    private BiConsumer<Agent, String> newDriverCallback;
    private int newDrivers;

    public TaxiServiceBehaviour(TaxiService agent, BiConsumer<Agent, String> newDriverCallback) {
        super(agent, PERIOD);
        service = agent;
        this.newDriverCallback = newDriverCallback;
    }

    @Override
    protected void onTick() {
        ReentrantReadWriteLock lock = service.getStorageLock();
        lock.writeLock().lock();
        List<Long> times = service.getWaitingTimes();
        double averageWaitingTime = (double) times.stream().reduce(0L, Long::sum) / times.size();
        averageWaitingTime *= Main.MODELLING_SPEED;
        times.clear();
        lock.writeLock().unlock();

        MyLog.log(service + " average waiting time " + averageWaitingTime);

        if (averageWaitingTime > WAITING_THRESHOLD) {
            MyLog.log("Hiring new driver");
            newDriverCallback.accept(new Driver(), "new_driver_" + newDrivers++);
        }
    }
}