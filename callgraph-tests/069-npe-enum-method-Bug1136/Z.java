// Issue 1136
// We are getting NPE while calculating class hash from enum method hash
// As we do not process enum at all, I've adeed to checks to skip enum's method
// calculation in CallGraphProcessor::processAllMethodInvocations and
// SummaryGraph::processFromMethodSummary.

// This test is intentionally turned off as we do not get method binding for enum methods.
// But in project we do get binding.

enum TrafficSignal {
    // This will call enum constructor with one
    // String argument
    RED("STOP"), GREEN("GO"), ORANGE("SLOW DOWN");

    // declaring private variable for getting values
    private String action;

    // getter method
    public String getAction() {
        show(this.action);
        return this.action;
    }

    // enum constructor - cannot be public or protected
    private TrafficSignal(String action) {
        this.action = action;
    }

    public static void show(String msg) {
        System.out.println("Can you see now? " + msg);
    }

}

public class Z {

    public static void main(String[] args) { /* <<< 29,5,36,5,callee,TrafficSignal:getAction()V */
        TrafficSignal[] signals = TrafficSignal.values();
        for (TrafficSignal signal : signals) {
            signal.getAction();
            // use getter method to get the value
            System.out.println("name : " + signal.name() + " action: " + signal.getAction());
        }
    }

}
