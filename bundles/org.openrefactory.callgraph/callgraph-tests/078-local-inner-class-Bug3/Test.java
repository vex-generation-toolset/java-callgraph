// Issue 3
// Extended call graph entry for local inner class

package org.openrefactory.test;

class Outer {
    void display() {
        String localVar = "Local Inner Access";

        class LocalInner {
            void show() {
                System.out.println(localVar);
            }
        }

        LocalInner inner = new LocalInner();
        inner.show();
    }

    public static void main(String[] args) {
        new Outer().display();
    }
}

/*$$$$$ org.openrefactory.test.Outer.main(String[]), 
   2,
   org.openrefactory.test.Outer.<init>(),
   org.openrefactory.test.Outer#display(),
 */

/*$$$$$ org.openrefactory.test.Outer#display(), 
  2,
  org.openrefactory.test.Outer#display().LocalInner.<init>(),
  org.openrefactory.test.Outer#display().LocalInner#show()
*/

/*$$$$$ org.openrefactory.test.Outer.<init>(), 
  0, 
*/

/*!!!!! org.openrefactory.test.Outer.main(String[]), 2, 431,11, 431,21 */
/*!!!!! org.openrefactory.test.Outer#display(), 2, 331,16, 357,12 */
/*!!!!! org.openrefactory.test.Outer.<init>(), 0 */
