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

/*$$$$$ Example1.main(String[]), 
   1, Example1.printNumbers(int[]...)
 */

/*$$$$$ Example1.main2(String[]), 
  1, Example1.printNumbers(int[]...)
*/

/*!!!!! Example1.main(String[]), 3, 293, 15, 345, 27, 241,14 */

/*!!!!! Example1.main2(String[]), 1, 460,14*/
