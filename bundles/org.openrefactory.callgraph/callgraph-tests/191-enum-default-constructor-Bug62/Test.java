// Issue 62
// Test for enum with default constructor

public enum Day {
    MONDAY, TUESDAY;

    public void info() {
        Day d = Day.MONDAY;
    }
}

/*$$$$$ Day#info(), 1,
7,5, 1, Day.<staticinit>()
*/
