package ua.kpi.properties;

public class CitizenState {
    public enum State {
        AT_HOME,
        WAIT_FOR_GUEST,
        WITH_GUEST,

        GUEST,
        GUEST_WAITING_RESPONSES,
        GUEST_TRAVELING
    }

    private State value;

    public CitizenState() {
        this.value = State.AT_HOME;
    }

    public void setValue(State stateValue) {
        this.value = stateValue;
    }

    public void setAtHome() {
        this.value = State.AT_HOME;
    }

    public void setGuest() {
        this.value = State.GUEST;
    }

    public State getValue() {
        return this.value;
    }

    public boolean isAtHome() {
        return value == State.AT_HOME || value == State.WAIT_FOR_GUEST;
    }
}
