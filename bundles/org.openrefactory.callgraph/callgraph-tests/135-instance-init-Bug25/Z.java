// Issue 25
// Instance Initialization Block

import java.util.Random;

public class Example4 {
    private int id;

    {
        // instance initializer block — can call library methods
        id = new Random().nextInt(1000);
    }

    public static void main(String[] args) {
        Example4 e = new Example4();
        System.out.println("Generated ID: " + e.id);
    }
}

/*$$$$$
  Example4.main(String[]), 1,
  Example4.<init>()
*/


/*$$$$$
  Example4.<init>(), 3,
  java.util.Random.<init>(),
  java.util.Random#Random(),
  java.util.Random#nextInt(int),
*/

/*!!!!! Example4.main(String[]), 1, 302, 14 */
/*!!!!! Example4.<init>(), 2, 201, 12, 201, 26 */
