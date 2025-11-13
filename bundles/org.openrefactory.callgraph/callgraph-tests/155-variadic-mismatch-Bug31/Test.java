// Issue 31
// A null pointer exception because a check was missing
// in method matching for variadic cases. Now not happening.

public class Example2 {

    static void printSum(Object label, int... nums) {
        int sum = 0;
        for (int n : nums) sum += n;
        System.out.println(label.toString() + ": " + sum);
    }

    static void printSum(int label, int... nums) {
        int sum = 0;
        for (int n : nums) sum += n;
        System.out.println(label + ": " + sum);
    }

    public static void main2(String[] args) {
        printSum("Empty");
    }
}

/*$$$$$ Example2.main2(String[]), 
  1, Example2.printSum(Object@@@int[]...)
*/

/*!!!!! Example2.main2(String[]),  1, 551, 17 */

// Previously, we were trying to match with two printSum cases.
// The first one is obviously false. But we did not filter it out.
// We were supposed to seek better match cases when we have a maybe match,
// but here we allowed that to continue even for false matches, because
// of the missing check. Now that check is added.
