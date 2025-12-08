// Issue 3
// Extended call graph entry for member inner class (non-static)

package org.openrefactory.test;

class Outer {
    private String message = "Hello from Outer";

    class Inner {
        void show() {
            System.out.println(message);
        }
    }

    public static void main(String[] args) {
        Outer outer = new Outer();
        Outer.Inner inner = outer.new Inner();
        inner.show();
    }
}


/*$$$$$ org.openrefactory.test.Outer.main(String[]), 3, 
   		16,23, 1, org.openrefactory.test.Outer.<init>(),
   		17,29, 1, org.openrefactory.test.Outer.Inner.<init>(),
   		18,9, 1, org.openrefactory.test.Outer.Inner#show(),
*/

/*$$$$$ org.openrefactory.test.Outer.Inner.<init>(), 0 */

/*$$$$$ org.openrefactory.test.Outer.<init>(), 0 */

/*$$$$$ org.openrefactory.test.Outer.Inner#show(), 0*/
