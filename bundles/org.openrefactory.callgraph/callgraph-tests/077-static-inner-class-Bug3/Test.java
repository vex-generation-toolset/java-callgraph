// Issue 3
// Extended call graph entry for static nested class

package org.openrefactory.test;

class Outer {
    private static String message = "Static Message";

    static class Nested {
        void show() {
            System.out.println(message);
        }
    }

    public static void main(String[] args) {
        Outer.Nested nested = new Outer.Nested();
        nested.show();
    }
}


/*$$$$$ org.openrefactory.test.Outer.main(String[]), 2,
		16,31, 1, org.openrefactory.test.Outer.Nested.<init>(),
		17,9, 1, org.openrefactory.test.Outer.Nested#show(),
*/

/*$$$$$ org.openrefactory.test.Outer.Nested.<init>(), 0 */

/*$$$$$ org.openrefactory.test.Outer.<init>(), 0 */

/*$$$$$ org.openrefactory.test.Outer.Nested#show(), 1,
  		10,9, 1, org.openrefactory.test.Outer.<staticinit>()
*/
