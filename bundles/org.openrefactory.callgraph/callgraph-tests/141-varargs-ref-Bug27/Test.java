// Issue 27
// Varargs with reference types

public class Example3 {
    static void joinStrings(String... parts) {
        String joined = String.join(" ", parts);
        System.out.println(joined);
    }

    public static void main(String[] args) {
        joinStrings("Java", "supports", "varargs!");
        joinStrings(); // prints nothing
    }
    
    public static void main2(String[] args) {
        joinStrings(); // prints nothing
    }
}

/*$$$$$ Example3.main(String[]), 
   1, Example3.joinStrings(String[]...)
 */

/*$$$$$ Example3.main2(String[]), 
  1, Example3.joinStrings(String[]...)
*/

/*!!!!! Example3.main(String[]),  2, 261,43, 314,13 */

/*!!!!! Example3.main2(String[]),  1, 412,13 */
