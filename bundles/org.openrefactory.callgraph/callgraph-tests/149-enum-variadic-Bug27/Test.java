// Issue 27
// Varargs with enums or custom classes

enum Priority { LOW, MEDIUM, HIGH }

public class Example8 {
    static void setPriorities(Priority... priorities) {
        for (Priority p : priorities)
            System.out.println("Set: " + p);
    }

    public static void main(String[] args) {
        setPriorities(Priority.LOW, Priority.HIGH);
    }
}

/*$$$$$ Example8.main(String[]), 2,
   		13,9, 1, Example8.setPriorities(Priority[]...),
   		12,5, 1, Priority.<staticinit>()
*/
