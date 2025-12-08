// Issue 27
// Varargs in constructors

import java.util.*;

public class Example9 {
    private List<String> items;

    Example9(String... items) {
        this.items = Arrays.asList(items);
    }

    void show() {
        System.out.println(items);
    }

    public static void main(String[] args) {
        new Example9("apple", "banana").show();
        new Example9().show(); // empty list
    }
}

/*$$$$$ Example9.main(String[]), 2,
   		18,9, 2, Example9#Example9(String[]...), Example9#show(),
   		19,9, 2, Example9.<init>(), Example9#show()
*/

// For the second empty constructor, we shall match with the default constructor here
// instead of the Example9(String... items) constructor.
