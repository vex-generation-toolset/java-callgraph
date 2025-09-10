// Issue 3
// Extended call graph entry for a super method call

package org.openrefactory.test;

class Parent {
    void work() {
        System.out.println("Parent is working");
    }
}

class Child extends Parent {
    @Override
    void work() {
        super.work(); // use parent version first
        System.out.println("Child adds more work");
    }

    public static void main(String[] args) {
        new Child().work();
    }
}



/*$$$$$ org.openrefactory.test.Child.main(String[]), 
   2,
   org.openrefactory.test.Child.<init>(),
   org.openrefactory.test.Child#work(),
 */

/*$$$$$ org.openrefactory.test.Child#work(), 
   1,
   org.openrefactory.test.Parent#work(),
*/
