// Issue 25
// Anonymous Inner Class

import java.util.Timer;

public class Example4 {
    void demo() {
        Timer timer = new Timer() {
            @Override
            public void cancel() {
                System.out.println("Timer canceled!");
                super.cancel();
            }
        };
    }
}

/*$$$$$ Example4#demo(), 1,
  		8,23, 1, Example4#demo()$Timer$1.<init>()
*/
