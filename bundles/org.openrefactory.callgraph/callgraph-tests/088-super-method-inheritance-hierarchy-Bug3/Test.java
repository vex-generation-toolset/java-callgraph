// Issue 3
// Extended call graph entry for a super method call in an inheritance hierarchy

package org.openrefactory.test;

class Grandparent {
    void say() { System.out.println("Grandparent"); }
}

class Parent extends Grandparent {
    void say() { System.out.println("Parent"); }
}

class Child extends Parent {
    void say() {
        super.say(); // calls Parent.say()
        System.out.println("Child");
    }
    public static void main(String[] args) {
        new Child().say();
    }
}


/*$$$$$ org.openrefactory.test.Child.main(String[]), 1,
   		20,9, 2, org.openrefactory.test.Child.<init>(), org.openrefactory.test.Child#say()
*/

/*$$$$$ org.openrefactory.test.Child#say(), 1,
   		16,9, 1, org.openrefactory.test.Parent#say(),
*/
