// Issue 27
// Varargs with arrays

public class Example4 {
    static void printAll(String... items) {
        for (String item : items)
            System.out.println(item);
    }

    public static void main(String[] args) {
        String[] arr = {"A", "B", "C"};
        printAll(arr); // array passed directly
    }
}

/*$$$$$ Example4.main(String[]), 1,
		12,9, 1, Example4.printAll(String[]...)
*/
