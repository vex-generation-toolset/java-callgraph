// Issue 3
// Extended call graph entry for an overloaded super method call

package org.openrefactory.test;

class Parent {
    void show() {
        System.out.println("Parent show()");
    }
    void show(String msg) {
        System.out.println("Parent show(String): " + msg);
    }
}

class Child extends Parent {
    void show() {
        super.show("Hello from Child"); // calls parent's overloaded method
        System.out.println("Child show()");
    }

    public static void main(String[] args) {
        new Child().show();
    }
}


/*$$$$$ org.openrefactory.test.Child.main(String[]), 
   2,
   org.openrefactory.test.Child.<init>(),
   org.openrefactory.test.Child#show(),
 */

/*$$$$$ org.openrefactory.test.Child#show(), 
   1,
   org.openrefactory.test.Parent#show(String),
*/
