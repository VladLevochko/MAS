package ua.kpi;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class Main {
    public static final int MODELLING_SPEED = 500;

    public static void main(String[] args) throws StaleProxyException {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        City city = new City(City.Type.BILA_TSERKVA, mainContainer);
        city.start();
    }
}
