// Issue 3
// Extended call graph entry for a method in an inner class 
// with fully qualified name.

package org.test;

public class Outer {
    private String message = "Hello from Outer class!";

    // Inner class
    class Inner {
        public void display() {
            // Call a method from the outer class
            System.out.println("Inner class calling method from Outer:");
            printMessage();  // <-- method call inside inner class
        }
    }

    // Method in Outer class
    public void printMessage() {
        System.out.println(message);
    }

    // Main method
    public static void main(String[] args) {
        // Create instance of outer class
        Outer outer = new Outer();
        
        // Create instance of inner class using outer object
        Outer.Inner inner = outer.new Inner();
        
        // Call method inside inner class
        inner.display();
    }
}

/*$$$$$ org.test.Outer.Inner#display(), 1, org.test.Outer#printMessage() */

/*$$$$$ org.test.Outer#printMessage(), 0 */

/*$$$$$ org.test.Outer.main(String[]), 3, 
     org.test.Outer.<init>(), 
     org.test.Outer.Inner.<init>(), 
     org.test.Outer.Inner#display() */
