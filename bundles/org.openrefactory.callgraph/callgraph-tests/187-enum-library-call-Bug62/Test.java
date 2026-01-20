// Issue 62
// Test for library call inside enum

public enum Logger {
    INSTANCE;

    public void log(String message) {
        Math.sqrt(16.0);
    }
}

/*$$$$$ Logger#log(String), 1,
8,9, 1, java.lang.Math.sqrt(double)
*/
