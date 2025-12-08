// Issue 25
// Static Field Depending on Another Library Object

import java.util.Locale;

public class Example7 {
    private static final String COUNTRY = Locale.getDefault().getDisplayCountry();

    public static void main(String[] args) {
        System.out.println("Running in: " + COUNTRY);
    }
}

/*$$$$$ Example7.main(String[]), 1,
  		9,5, 1, Example7.<staticinit>(),
*/

/*$$$$$ Example7.<staticinit>(), 1,
  		7,43, 2, java.util.Locale.getDefault(), Locale#getDisplayCountry()
*/
