package ua.kpi;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

import java.util.List;


public class Main {
    public static final int MODELLING_SPEED = 2000;
    public static CityType CITY_TYPE = CityType.UZHHOROD;
    public static ModelingParams MODELLING_PARAMS = ModelingParams.VARIANT_1;

    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        City city = new City(CITY_TYPE, mainContainer);
//        city.start();
        List<double[]> results = city.simulate();
        for (double[] result : results) {
            System.out.println(String.format("%f %f %f", result[0], result[1], result[2]));
        }
    }
}
