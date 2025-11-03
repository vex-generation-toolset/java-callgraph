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



/*$$$$$ Example9.main(String[]), 
   3,
   Example9#Example9(String[]...),
   Example9#show(),
   Example9.<init>()
 */

/*!!!!! Example9.main(String[]),  4, 315,31, 315, 38, 363,21,  363,14 */

// For the second empty constructor, we shall match with the default constructor here
// instead of the Example9(String... items) constructor.
