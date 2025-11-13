// Issue 3
// Extended call graph entry for deeply nested inner class

package org.openrefactory.test;

class Outer {
    class Inner1 {
        class Inner2 {
            class Inner3 {
                void show() {
                    System.out.println("Deep Nested Inner Class");
                }
            }
        }
    }

    public static void main(String[] args) {
        Outer outer = new Outer();
        Outer.Inner1 inner1 = outer.new Inner1();
        Outer.Inner1.Inner2 inner2 = inner1.new Inner2();
        Outer.Inner1.Inner2.Inner3 inner3 = inner2.new Inner3();
        inner3.show();
    }
}

/*$$$$$ org.openrefactory.test.Outer.main(String[]), 
   5,
   org.openrefactory.test.Outer.<init>(),
   org.openrefactory.test.Outer.Inner1.<init>(),
   org.openrefactory.test.Outer.Inner1.Inner2.<init>(),
   org.openrefactory.test.Outer.Inner1.Inner2.Inner3.<init>(),
   org.openrefactory.test.Outer.Inner1.Inner2.Inner3#show(),
 */

/*!!!!! org.openrefactory.test.Outer.main(String[]), 5, 400,11, 443,18, 500,19, 565,19, 594,13 */
