package ua.kpi;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;


public class Main {
    public static final int MODELLING_SPEED = 500;

    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        City city = new City(City.Type.UZHHOROD, mainContainer);
        city.start();
    }
}
