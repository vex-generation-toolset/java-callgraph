// Issue 62
// Test for enum with abstract methods

public enum Operation {
    PLUS {
        public double eval(double x, double y) { return x + y; }
    },
    MINUS {
        public double eval(double x, double y) { return x - y; }
    },
    TIMES {
        public double eval(double x, double y) { return x * y; }
    };

    public abstract double eval(double x, double y);

    public void demo() {
        Operation op = Operation.PLUS;
        double result = op.eval(10, 20);
    }
}

/*$$$$$ Operation#demo(), 2,
19,25, 1, Operation$Operation$1#eval(double@@@double),
17,5, 1, Operation.<staticinit>()
*/
