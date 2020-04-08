package ua.kpi.behaviors;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import ua.kpi.Main;
import ua.kpi.MyLog;
import ua.kpi.agents.Driver;
import ua.kpi.agents.TaxiService;

import java.util.List;
import java.util.Map;
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

        List<Double> times = service.getWaitingTimes();
        int tripsNumber = times.size();
        double totalWaitingTime = times.stream().reduce(0D, Double::sum);
        times.clear();

        Map<String, Integer> trips = service.getTrips();
        StringBuilder sb = new StringBuilder("Trips number:\n");
        for (String driverName : trips.keySet()) {
            sb.append(String.format("%s: %d\n", driverName, trips.get(driverName)));
        }
        trips.clear();

        lock.writeLock().unlock();

        double averageWaitingTime = totalWaitingTime * Main.MODELLING_SPEED / tripsNumber;
        MyLog.log(String.format("%s average waiting time: %f; trips number: %d",
                service, averageWaitingTime, tripsNumber));
        MyLog.log(sb.toString());

        if (averageWaitingTime > WAITING_THRESHOLD) {
            MyLog.log("Hiring new driver");
            newDriverCallback.accept(new Driver(), "new_driver_" + newDrivers++);
        }
    }
}
