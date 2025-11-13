// Issue 25
// Default cons to field init link

import java.time.LocalDate;

public class Example1 {
    // Instance field initialized using a library method call
    private LocalDate createdOn = LocalDate.now(); // calls static method

    // Another example: uses result of a library method
    private int hash = "hello".hashCode(); // calls instance method of String

    public void show() {
        System.out.println("Created on: " + createdOn + ", hash: " + hash);
    }

    public static void main(String[] args) {
        new Example1().show();
    }
}

/*$$$$$
  Example1.main(String[]), 2,
  Example1.<init>(),
  Example1#show()
*/


/*$$$$$
  Example1#show(), 0,
*/

// Linking default constructor to library method calls for field initialization.

/*$$$$$
  Example1.<init>(), 1,
  java.time.LocalDate.now(),
*/

/*!!!!! Example1#show(), 0 */
/*!!!!! Example1.<init>(), 1, 197,15 */
