// Issue 27
// Case of variadic parameter in formal but none in actual
// The matching upto variadic is partial.

public class Example2 {
    static void printSum(Object label, int... nums) {
        int sum = 0;
        for (int n : nums) sum += n;
        System.out.println(label.toString() + ": " + sum);
    }
    
    public static void main2(String[] args) {
        printSum("Empty");
    }
}

/*$$$$$ Example2.main2(String[]), 
  1, Example2.printSum(Object@@@int[]...)
*/

/*!!!!! Example2.main2(String[]),  1, 376,17 */
