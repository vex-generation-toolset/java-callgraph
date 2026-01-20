// Issue 62
// Test for implicit methods of enum

public enum Color {
    RED, GREEN, BLUE;

    public void execute() {
        Color[] colors = Color.values();
        Color red = Color.valueOf("RED");
    }
}

/*$$$$$ Color#execute(), 2,
8,26, 1, Color.values(),
9,21, 1, Color.valueOf(String)
*/
