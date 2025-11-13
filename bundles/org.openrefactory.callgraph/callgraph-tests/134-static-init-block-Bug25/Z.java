// Issue 25
// Static Initialization Block

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Example3 {
    private static final String START_TIME;

    static {
        // calls multiple library methods
        START_TIME = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static void main(String[] args) {
        System.out.println("Started at: " + START_TIME);
    }
}

/*$$$$$
  Example3.main(String[]), 1,
  Example3.<staticinit>()
*/


/*$$$$$
  Example3.<staticinit>(), 2,
  java.time.LocalDateTime.now(),
  LocalDateTime#format(Object),
*/

/*!!!!! Example3.main(String[]), 1, 360,103 */
/*!!!!! Example3.<staticinit>(), 2, 265,19, 265,82 */
