// Issue 1958
// Generic Varargs
// Actually fixed by code change in 1955

public class Example5 {
    @SafeVarargs
    static <T> void printAll(T... elements) {
        for (T e : elements)
            System.out.println(e);
    }

    public static void main(String[] args) {
        printAll(1, 2, 3);
        printAll("A", "B", "C");
    }
}

/*$$$$$ Example5.main(String[]), 
   1, Example5.printAll(T[]...)
 */


/*!!!!! Example5.main(String[]), 2, 286, 17, 313,23 */
