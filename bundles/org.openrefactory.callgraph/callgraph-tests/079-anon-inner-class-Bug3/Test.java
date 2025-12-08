// Issue 3
// Extended call graph entry for anonymous inner class

package org.openrefactory.test;

abstract class Greeting {
    abstract void sayHello();
}

class Outer {
    public static void main(String[] args) {
        Greeting g = new Greeting() {  // anonymous inner class
            void sayHello() {
                System.out.println("Hello from Anonymous Inner Class");
            }
        };
        g.sayHello();
    }
}

/*$$$$$ org.openrefactory.test.Outer.main(String[]), 2,
		12,22, 1, org.openrefactory.test.Outer.main(String[])$Greeting$1.<init>(),
   		17,9, 1, org.openrefactory.test.Outer.main(String[])$Greeting$1#sayHello(),
*/
