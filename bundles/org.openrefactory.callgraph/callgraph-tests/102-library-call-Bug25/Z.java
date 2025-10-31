// Issue 25
// callable interface call from Thread

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Z {
    static String str;

    public void foo(ExecutorService pool) {
        MultithreadingDemo demo = new MultithreadingDemo();
        pool.submit(demo);
    }

    public void bar() {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        foo(pool);
    }
}

class MultithreadingDemo implements Callable<Integer>
{
    public Integer call()
    {
        Z.str = "Demo";
        System.out.println(Z.str);
        return 0;
    }
}

/*$$$$$
  Z#foo(ExecutorService), 2,
  MultithreadingDemo.<init>(),
  java.util.concurrent.ExecutorService#submit(MultithreadingDemo),
 */

/*$$$$$
  Z#bar(), 2,
  Z#foo(ExecutorService),
  java.util.concurrent.Executors.newFixedThreadPool(int)
*/

/*$$$$$
  MultithreadingDemo#call(), 1,
  Z.<staticinit>()
*/
