// Issue 62
// Test for enum implementing an interface

interface Identifiable {
    String getId();
}

public enum Status implements Identifiable {
    ACTIVE {
        @Override
        public String getId() {
            return "A";
        }
    },
    INACTIVE {
        @Override
        public String getId() {
            return "I";
        }
    };

    public void demo() {
        Identifiable active = Status.ACTIVE;
        active.getId();
    }
}

/*$$$$$ Status#demo(), 2,
22,5, 1, Status.<staticinit>(),
24,9, 1, Status$Status$1#getId()
*/
