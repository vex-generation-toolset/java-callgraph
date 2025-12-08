// Issue 27
// Overloaded method with and without varargs
// Actually fixed by code change in 1955

public class Example6 {
    static void display(int n) {
        System.out.println("Single int: " + n);
    }

    static void display(int... nums) {
        System.out.println("Varargs count: " + nums.length);
    }

    public static void main(String[] args) {
        display(5);         // prefers single int version
        display(1, 2, 3);   // varargs version
    }
}

/*$$$$$ Example6.main(String[]), 2,
		15,9, 1, Example6.display(int),
		16,9, 1, Example6.display(int[]...)
*/
