// Issue 25
// Extending a Library Thread Class

public class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Custom work");
        super.run();  // calls Thread.run() — which executes the Runnable target if any
    }

    public static void main(String[] args) {
        MyThread t = new MyThread();
        t.start();
    }
}

/*$$$$$
  MyThread#run(), 1,
  java.lang.Thread#run(),
*/


/*$$$$$
  MyThread.main(String[]), 1,
  MyThread.<init>(),
*/
