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

/*$$$$$ MyLogger#MyLogger(String@@@String), 2,
  		8,9, 1, java.util.logging.Logger#Logger(String@@@String),
  		7,5, 1, MyLogger.<init>()
*/


/*$$$$$	MyLogger#info(String), 1,
		14,9, 1, java.util.logging.Logger#info(String)
*/
