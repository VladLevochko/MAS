package ua.kpi;

import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import ua.kpi.agents.Citizen;
import ua.kpi.agents.Driver;
import ua.kpi.agents.TaxiService;
import ua.kpi.properties.AgentLocation;

import java.util.ArrayList;
import java.util.List;

public class City {

    class KillerThread extends Thread {
        public void run() {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (Agent agent : agents) {
                agent.doDelete();
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private CityType cityType;
    private AgentContainer container;

    private List<Double> values;
    private int counter;
    private int maxCount;
    private final Object monitor = new Object();
    private List<Agent> agents;

    public City(CityType cityType, AgentContainer container) {
        this.cityType = cityType;
        this.container = container;
        agents = new ArrayList<>();
        values = new ArrayList<>();
    }

    public List<double[]> simulate() {
        int minDriversNumber = cityType.drivers / 2;
        int maxDriversNumber = cityType.drivers * 2;
        double minCoefficient = 1.1;
        double maxCoefficient = 1.5;
        int days = 4;

        ArrayList<double[]> results = new ArrayList<>();

        for (int driversNumber = minDriversNumber; driversNumber < maxDriversNumber; driversNumber++) {
            for (double coefficient = minCoefficient; coefficient < maxCoefficient; coefficient += 0.1) {
                System.out.println("iteration");
                double averageIncome = getAverageIncome(driversNumber, coefficient, days);
                results.add(new double[] {driversNumber, coefficient, averageIncome});
            }
        }

        return results;
    }

    private double getAverageIncome(int driversNumber, double coefficient, int days) {
        values.clear();
        counter = 0;
        maxCount = days;

        agents.clear();
        createAgents(driversNumber, coefficient);
        try {
            KillerThread killer = new KillerThread();
            killer.start();
            killer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        double averageIncome = 0;
        for (int i = 0; i < values.size() - 1; i++) {
            averageIncome += values.get(i + 1) - values.get(i);
        }
        averageIncome /= (values.size() - 1);

        return averageIncome;
    }

    private void createAgents(int driversNumber, double coefficient) {
        TaxiService taxiService = TaxiService.getInstance();
        taxiService.setNewDriverCallback(this::addNewAgent);
        taxiService.addBalanceCheckCallback(this::update);
        taxiService.setCoefficient(coefficient);
        String nickname = "taxi_service";
        addNewAgent(taxiService, nickname);

        Agent agent;
        for (int i = 0; i < driversNumber; i++) {
            agent = new Driver();
            nickname = String.format("driver_%d", i);
            addNewAgent(agent, nickname);
            taxiService.updateBalance(-15000);
        }

        for (int i = 0; i < cityType.population; i++) {
            agent = new Citizen(generateLocation());
            nickname = String.format("citizen_%d", i);
            addNewAgent(agent, nickname);
        }
    }

    private synchronized void update(Double value) {
        values.add(value);
        counter++;
        if (counter != maxCount) {
            return;
        }

        synchronized (monitor) {
            monitor.notify();
        }
    }

    public void start() {
        TaxiService taxiService = TaxiService.getInstance();
        taxiService.setNewDriverCallback(this::addNewAgent);
        String nickname = "taxi_service";
        addNewAgent(taxiService, nickname);

        Agent agent;
        for (int i = 0; i < cityType.drivers; i++) {
            agent = new Driver();
            nickname = String.format("driver_%d", i);
            addNewAgent(agent, nickname);
            taxiService.updateBalance(-15000);
        }

        for (int i = 0; i < cityType.population; i++) {
            agent = new Citizen(generateLocation());
            nickname = String.format("citizen_%d", i);
            addNewAgent(agent, nickname);
        }
    }

    private AgentLocation generateLocation() {
        int x = (int) (Math.random() * cityType.width);
        int y = (int) (Math.random() * cityType.height);

        return new AgentLocation(x, y);
    }

    private void addNewAgent(Agent agent, String nickname) {
        try {
            AgentController controller = container.acceptNewAgent(nickname, agent);
            controller.start();
            agents.add(agent);
        } catch (StaleProxyException e) {
            MyLog.log("City can't create new agent " + nickname);
        }
    }
}
