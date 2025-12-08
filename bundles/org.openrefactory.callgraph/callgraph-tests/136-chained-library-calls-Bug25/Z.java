// Issue 25
// Chained Library Calls in Field Initialization

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Example6 {
    private String timestamp = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public static void main(String[] args) {
        System.out.println(new Example6().timestamp);
    }
}

/*$$$$$ Example6.main(String[]), 1,
  		12,28, 1, Example6.<init>(),
*/

/*$$$$$ Example6.<init>(), 2,
  		8,32, 2, java.time.LocalDateTime.now(), LocalDateTime#format(DateTimeFormatter),
  		9,41, 1, java.time.format.DateTimeFormatter.ofPattern(String)
*/
