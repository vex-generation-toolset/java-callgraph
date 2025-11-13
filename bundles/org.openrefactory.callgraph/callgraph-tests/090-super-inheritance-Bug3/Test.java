// Issue 3
// Extended call graph entry for a super method call in an inheritance hierarchy

package org.openrefactory.test;

class Parent {
    Parent(int x) { System.out.println("Parent int: " + x); }
    Parent(String s) { System.out.println("Parent String: " + s); }
}

class Child extends Parent {
    Child() {
        super(42); // choose which parent constructor to call
        System.out.println("Child constructor");
    }
}


/*$$$$$ org.openrefactory.test.Child#Child(), 
   2,
   org.openrefactory.test.Parent#Parent(int),
    org.openrefactory.test.Child.<init>(),
 */

/*!!!!! org.openrefactory.test.Child#Child(), 2, 325,10, 307,126 */
