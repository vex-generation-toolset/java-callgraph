// Issue 3
// Extended call graph entry for a super constructor call
// To a constructor with no parameters

package org.openrefactory.test;

class Parent {
    Parent() {
    }
}

class Child extends Parent {
    Child() {
        super();
        System.out.println("Child constructor");
    }

    public static void main(String[] args) {
        new Child();
    }
}



/*$$$$$ org.openrefactory.test.Child.main(String[]), 
   1,
   org.openrefactory.test.Child#Child(),
 */

/*$$$$$ org.openrefactory.test.Child#Child(), 
   1,
   org.openrefactory.test.Parent#Parent(),
*/
