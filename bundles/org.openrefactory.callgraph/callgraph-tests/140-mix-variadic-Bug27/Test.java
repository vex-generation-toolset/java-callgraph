// Issue 27
// Mixing normal parameters and varargs

public class Example2 {
    static void printSum(String label, int... nums) {
        int sum = 0;
        for (int n : nums) sum += n;
        System.out.println(label + ": " + sum);
    }

    public static void main(String[] args) {
        printSum("Total", 1, 2, 3);
        printSum("Empty");
    }
    
    public static void main2(String[] args) {
        printSum("Empty");
    }
}

/*$$$$$ Example2.main(String[]), 2,
		12,9, 1, Example2.printSum(String@@@int[]...),
		13,9, 1, Example2.printSum(String@@@int[]...)
*/

/*$$$$$ Example2.main2(String[]), 1,
		17,9, 1, Example2.printSum(String@@@int[]...)
*/
