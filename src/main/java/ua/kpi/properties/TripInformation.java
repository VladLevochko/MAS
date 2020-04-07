package ua.kpi.properties;

import java.io.Serializable;

public class TripInformation implements Serializable {
    private double distanceToPassenger;
    private double distanceToDestination;
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

    public void setDistanceToPassenger(double distance) {
        this.distanceToPassenger = distance;
    }

    public double getDistanceToPassenger() {
        return this.distanceToPassenger;
    }

    public void setDistanceToDestination(double distance) {
        this.distanceToDestination = distance;
    }

    public double getDistanceToDestination() {
        return this.distanceToDestination;
    }

    public String toString() {
        return String.format("TripInformation { time [toPassenger: %f; toDestination: %f; total: %f] " +
                        "distance [toPassenger: %f; toDestination: %f; total: %f] }",
                timeToPassenger, timeToDestination, getTotalTime(),
                distanceToPassenger, distanceToDestination, distanceToPassenger + distanceToDestination);
    }
}
