// Issue 62
// Test for enum with fields and custom constructor

public enum Level {
    LOW(1), MEDIUM(2), HIGH(3);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void demo() {
        Level level = Level.MEDIUM;
        int v = level.getValue();
    }
}

/*$$$$$ Level#demo(), 2,
17,5, 1, Level.<staticinit>(),
19,17, 1, Level#getValue()
*/
