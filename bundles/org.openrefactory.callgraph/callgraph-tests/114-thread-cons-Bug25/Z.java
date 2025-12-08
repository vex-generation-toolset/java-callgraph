// Issue 25
// Thread and Runnable Example

public class Example10 {
    void demo() {
        Thread t = new Thread(() -> System.out.println("Running!"));
        t.start();
    }
}

// We do not need import for things such as Thread.

// The lambda type is not found. It is identified as an Object.

/*$$$$$ Example10#demo(), 2,
  		6,20, 1, java.lang.Thread#Thread(Object),
  		7,9, 1, java.lang.Thread#start(),
*/
