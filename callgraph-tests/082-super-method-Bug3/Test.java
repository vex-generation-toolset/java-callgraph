// Issue 3
// Extended call graph entry for a super method invocation

package org.openrefactory.test;

class Parent {
    void greet() {
        System.out.println("Hello from Parent");
    }
}

class Child extends Parent {
    void greet() {
        super.greet(); // calls Parent.greet()
        System.out.println("Hello from Child");
    }

    public static void main(String[] args) {
        new Child().greet();
    }
}



/*$$$$$ org.openrefactory.test.Child.main(String[]), 
   2,
   org.openrefactory.test.Child.<init>(),
   org.openrefactory.test.Child#greet(),
 */

/*$$$$$ org.openrefactory.test.Child#greet(), 
   1,
   org.openrefactory.test.Parent#greet(),
*/

/*$$$$$ org.openrefactory.test.Parent#greet(), 
   0
*/
