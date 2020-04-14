package ua.kpi.behaviors;

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
import java.util.function.Consumer;

public class TaxiServiceBehaviour extends TickerBehaviour {
    private static long PERIOD = 24 * 60 * 60 * 1000 / Main.MODELLING_SPEED;
    private static double USED_CAR_PRICE = 9000;
    private long WAITING_THRESHOLD = 60 * 60 * 1000;

    private TaxiService service;
    private BiConsumer<Agent, String> newDriverCallback;
    private Consumer<Double> balanceCheckCallback;
    private int newDrivers;

    public TaxiServiceBehaviour(TaxiService agent, BiConsumer<Agent, String> newDriverCallback) {
        super(agent, PERIOD);
        service = agent;
        this.newDriverCallback = newDriverCallback;
    }

    public void setBalanceCheckCallback(Consumer<Double> callback) {
        this.balanceCheckCallback = callback;
    }

    @Override
    protected void onTick() {
        ReentrantReadWriteLock lock = service.getStorageLock();
        lock.writeLock().lock();

        List<Double> times = service.getWaitingTimes();
        int tripsNumber = times.size();
        double totalWaitingTime = times.stream().reduce(0D, Double::sum);
        times.clear();
        List<Integer> tries = service.getRequestingTries();
        double triesSum = tries.stream().reduce(0, Integer::sum);
        tries.clear();

        StringBuilder sb = new StringBuilder();

        double averageWaitingTime = totalWaitingTime / tripsNumber;
        double averageTriesNumber = triesSum / tripsNumber;
        sb.append(String.format("%s average waiting time: %dm; average tries: %f; trips number: %d\n",
                service, Math.round(averageWaitingTime / 60), averageTriesNumber, tripsNumber));

        Map<String, Integer> trips = service.getTrips();
        sb.append("Trips number:\n");
        for (String driverName : trips.keySet()) {
            sb.append(String.format("%s: %d\n", driverName, trips.get(driverName)));
        }
        trips.clear();

        lock.writeLock().unlock();

        double balance = calculateBalance();
        sb.append("Company balance ").append(balance);
        if (balanceCheckCallback != null) {
            balanceCheckCallback.accept(balance);
        }

        MyLog.log(sb.toString());

        if (averageWaitingTime > WAITING_THRESHOLD) {
            MyLog.log("Hiring new driver");
            newDriverCallback.accept(new Driver(), "new_driver_" + newDrivers++);
        }
    }

    private double calculateBalance() {
        return service.getBalance() + service.getDrivers().size() * USED_CAR_PRICE;
    }
}
