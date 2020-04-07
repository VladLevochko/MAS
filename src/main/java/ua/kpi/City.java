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
        UZHHOROD(7900, 8100, 115, 4),
        BILA_TSERKVA(200, 200, 5, 2),
        CHABANY(100, 100, 2, 1);

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

    public void start() {
        Agent agent = TaxiService.getInstance();
        ((TaxiService) agent).setNewDriverCallback(this::addNewAgent);
        String nickname = "taxi_service";
        addNewAgent(agent, nickname);

        for (int i = 0; i < cityType.drivers; i++) {
            agent = new Driver();
            nickname = String.format("driver_%d", i);
            addNewAgent(agent, nickname);
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
        } catch (StaleProxyException e) {
            MyLog.log("City can't create new agent " + nickname);
        }
    }
}
