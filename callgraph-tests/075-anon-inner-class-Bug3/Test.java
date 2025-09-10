// Issue 3
// Extended call graph entry for a method in an anonymous inner class 
// that calls the default constructor of the outer class.

package org.openrefactory.test;

class Polygon {
    String x;
    String y;
    
   public String getVal() {
      return x;
   }
}

class AnonymousDemo {
   Polygon p1;
   String m1;
   
   public void foo2() {
       p0 = new Polygon() {
           public String getVal() {
               return x;
            }
       };
       p1 = new Polygon() {
           public String getVal() {
               return y;
            }
       };
       m1 = p1.getVal();
    }
}

/*$$$$$ org.openrefactory.test.AnonymousDemo#foo2(), 
   5, 
   org.openrefactory.test.AnonymousDemo#foo2()$Polygon$1.<init>(), 
   org.openrefactory.test.AnonymousDemo#foo2()$Polygon$2.<init>(), 
   org.openrefactory.test.AnonymousDemo#foo2()$Polygon$1#getVal(),
   org.openrefactory.test.AnonymousDemo#foo2()$Polygon$2#getVal(),
   org.openrefactory.test.Polygon#getVal() 
 */

/*$$$$$ org.openrefactory.test.AnonymousDemo#foo2()$Polygon$1.<init>(), 
  1,
  org.openrefactory.test.Polygon.<init>() 
*/

/*$$$$$ org.openrefactory.test.AnonymousDemo#foo2()$Polygon$2.<init>(), 
  1,
  org.openrefactory.test.Polygon.<init>() 
*/
