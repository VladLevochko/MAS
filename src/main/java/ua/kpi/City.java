package ua.kpi;

import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import ua.kpi.agents.Citizen;
import ua.kpi.agents.Driver;
import ua.kpi.agents.TaxiService;
import ua.kpi.properties.AgentLocation;

public class City {
    public enum Type {
        KYIV(29000, 30000, 3000, 150),
        ZHYTOMYR(9000, 7200, 280, 15),
        TEST(10, 10, 2, 1);

        int width;
        int height;
        int population;
        int drivers;

        Type(int width, int height, int population, int drivers) {
            this.width = width;
            this.height = height;
            this.population = population;
            this.drivers = drivers;
        }
    }

    private Type cityType;
    private AgentContainer container;

    public City(Type cityType, AgentContainer container) {
        this.cityType = cityType;
        this.container = container;
    }

    public void start() throws StaleProxyException {
        Agent agent = TaxiService.getInstance();
        String nickname = "taxi_service";
        AgentController controller = container.acceptNewAgent(nickname, agent);
        controller.start();

        for (int i = 0; i < cityType.drivers; i++) {
            agent = new Driver();
            nickname = String.format("driver %d", i);
            controller = container.acceptNewAgent(nickname, agent);
            controller.start();
        }

        for (int i = 0; i < cityType.population; i++) {
            agent = new Citizen(generateLocation());
            nickname = String.format("citizen %d", i);
            controller = container.acceptNewAgent(nickname, agent);
            controller.start();
        }
    }

    private AgentLocation generateLocation() {
        int x = (int) (Math.random() * cityType.width);
        int y = (int) (Math.random() * cityType.height);

        return new AgentLocation(x, y);
    }

}
