package ua.kpi.properties;

import java.io.Serializable;

public class AgentLocation implements Serializable {
    private int x;
    private int y;

    public AgentLocation() {
        x = 0;
        y = 0;
    }

    public AgentLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public double distanceTo(AgentLocation other) {
        double xDifference = this.x - other.getX();
        double yDifference = this.y - other.getY();

        return Math.sqrt(xDifference * xDifference + yDifference * yDifference);
    }
}
