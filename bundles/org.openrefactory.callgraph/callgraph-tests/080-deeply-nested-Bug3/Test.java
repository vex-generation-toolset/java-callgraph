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

/*$$$$$ org.openrefactory.test.Outer.main(String[]), 5,
   		18,23, 1, org.openrefactory.test.Outer.<init>(),
   		19,31, 1, org.openrefactory.test.Outer.Inner1.<init>(),
   		20,38, 1, org.openrefactory.test.Outer.Inner1.Inner2.<init>(),
   		21,45, 1, org.openrefactory.test.Outer.Inner1.Inner2.Inner3.<init>(),
   		22,9, 1, org.openrefactory.test.Outer.Inner1.Inner2.Inner3#show()
*/
