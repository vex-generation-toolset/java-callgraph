// Issue 25
// Instance Field Initialized from a Library Collection

import java.util.List;

public class Example8 {
    private List<String> colors = List.of("red", "green", "blue"); // List.of is library method

    public static void main(String[] args) {
        System.out.println(new Example8().colors);
    }
}

/*$$$$$
  Example8.main(String[]), 1,
  Example8.<init>(),
*/


/*$$$$$
  Example8.<init>(), 1,
  java.util.List.of(String@@@String@@@String),
*/

/*!!!!! Example8.main(String[]), 1, 286,14 */
/*!!!!! Example8.<init>(), 1, 151,31 */
