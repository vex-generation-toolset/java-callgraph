// Issue 62
// Test for enum nested in a generic class

public class Generic<T> {
    public enum Type {
        A, B;
    }

    public T process(Type t) {
        return null;
    }

    public void demo() {
        process(Type.A);
    }
}

/*$$$$$ Generic#demo(), 2,
13,5, 1, Generic.Type.<staticinit>(),
14,9, 1, Generic#process(Type)
*/
