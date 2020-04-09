package ua.kpi.properties;

public enum DriverState {
    FREE("free"),
    WAITING_FOR_PASSENGER("waiting_for_passenger"),
    DRIVING("driving");

    String value;

    DriverState(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DriverState{" + value + '}';
    }
}
