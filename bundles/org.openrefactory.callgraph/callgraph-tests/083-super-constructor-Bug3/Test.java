// Issue 3
// Extended call graph entry for a super constructor call

package org.openrefactory.test;

class Parent {
    Parent(String msg) {
        System.out.println("Parent constructor: " + msg);
    }
}

class Child extends Parent {
    Child() {
        super("Called from Child"); // must be first
        System.out.println("Child constructor");
    }

    public static void main(String[] args) {
        new Child();
    }
}

/*$$$$$ org.openrefactory.test.Child.main(String[]), 1,
   		19,9, 1, org.openrefactory.test.Child#Child(),
*/

/*$$$$$ org.openrefactory.test.Child#Child(), 2,
   		14,9, 1, org.openrefactory.test.Parent#Parent(String),
   		13,5, 1, org.openrefactory.test.Child.<init>(),
*/
