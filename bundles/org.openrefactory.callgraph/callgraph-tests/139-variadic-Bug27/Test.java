// Issue 27
// Variadic methods

public class Example1 {
    static void printNumbers(int... numbers) {
        for (int n : numbers) {
            System.out.println(n);
        }
    }

    public static void main(String[] args) {
        printNumbers();             // no arguments
        printNumbers(1);            // one argument
        printNumbers(1, 2, 3, 4, 5);// multiple arguments
    }
    
    public static void main2(String[] args) {
        printNumbers();             // no arguments
    }
}

/*$$$$$ Example1.main(String[]), 3,
		12,9, 1, Example1.printNumbers(int[]...),
		13,9, 1, Example1.printNumbers(int[]...),
		14,9, 1, Example1.printNumbers(int[]...)
*/

/*$$$$$ Example1.main2(String[]), 1,
		18,9, 1, Example1.printNumbers(int[]...)
*/
