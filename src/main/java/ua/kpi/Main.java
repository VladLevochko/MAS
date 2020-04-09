package ua.kpi;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;


public class Main {
    public static final int MODELLING_SPEED = 1000;
    public static City.Type CITY_TYPE = City.Type.UZHHOROD;

    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        City city = new City(CITY_TYPE, mainContainer);
        city.start();
    }
}
