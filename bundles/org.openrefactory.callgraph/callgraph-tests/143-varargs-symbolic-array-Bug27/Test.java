// Issue 1958
// Generic Varargs and array
// Actually fixed by code change in 1955

public class Example4 {
    static <T> void printAll(T... items) {
        for (T item : items)
            System.out.println(item);
    }

    public static void main(String[] args) {
        String[] arr = {"A", "B", "C"};
        printAll(arr); // array passed directly
    }
}

/*$$$$$ Example4.main(String[]), 1,
		13,9, 1, Example4.printAll(Object[]...)
*/
