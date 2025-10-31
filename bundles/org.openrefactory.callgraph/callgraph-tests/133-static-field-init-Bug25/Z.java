// Issue 25
// Static Field Initialization (calls library method)

import java.util.UUID;

public class Example2 {
    // Static field initialized by calling library method
    private static final String APP_ID = UUID.randomUUID().toString();

    public static void main(String[] args) {
        System.out.println("App ID: " + APP_ID);
    }
}

/*$$$$$
  Example2.main(String[]), 1,
  Example2.<staticinit>()
*/


/*$$$$$
  Example2.<staticinit>(), 2,
  java.util.UUID.randomUUID(),
  UUID#toString(),
*/

// The last bit is perhaps questionable. Should we get the class of the chain method inside a library?
// Or should we just content with somethidn hard?
