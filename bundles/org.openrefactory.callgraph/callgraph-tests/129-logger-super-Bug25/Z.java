// Issue 25
// Calling a Library Superclass’s Implementation Inside a Template Method

import java.util.logging.Logger;

public class MyLogger extends Logger {
    protected MyLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    @Override
    public void info(String msg) {
        System.out.println("Custom logging: " + msg);
        super.info(msg);  // goes to Logger.info()
    }
}

/*$$$$$
  MyLogger#MyLogger(String@@@String), 2,
  java.util.logging.Logger#Logger(String@@@String),
  MyLogger.<init>()
*/


/*$$$$$
MyLogger#info(String), 1,
java.util.logging.Logger#info(String)
*/
