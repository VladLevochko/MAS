package ua.kpi.properties;

import java.io.Serializable;

public class TripInformation implements Serializable {
    private double timeToPassenger;
    private double timeToDestination;

    public TripInformation() {

    }

    public TripInformation(double timeToPassenger, double timeToDestination) {
        this.timeToPassenger = timeToPassenger;
        this.timeToDestination = timeToDestination;
    }

    public double getTimeToPassenger() {
        return timeToPassenger;
    }

    public void setTimeToPassenger(double timeToPassenger) {
        this.timeToPassenger = timeToPassenger;
    }

    public double getTimeToDestination() {
        return timeToDestination;
    }

    public void setTimeToDestination(double timeToDestination) {
        this.timeToDestination = timeToDestination;
    }

    public double getTotalTime() {
        return timeToDestination + timeToPassenger;
    }
}
