// Issue 62
// Test for enum with mix of implicit and explicit method calls

public enum State {
    ON, OFF;

    public void turnOn() {
        // Explicit call
        log("Turning on");
        // Implicit calls
        State[] states = State.values();
        State s = State.valueOf("ON");
    }

    private void log(String msg) {
        // ...
    }
}

/*$$$$$ State#turnOn(), 3,
9,9, 1, State#log(String),
11,26, 1, State.values(),
12,19, 1, State.valueOf(String)
*/
