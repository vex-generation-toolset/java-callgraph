// Issue 3
// Extended call graph entry for a method in an anonymous inner class 

class Polygon {
    String x;
    String y;
    
    Polygon () {
        x = "00";
        y = "00";
    }
    
    Polygon (String x, String y) {
        this.x = x;
        this.y = y;
    }
    
   public String getVal() {
      return x;
   }
}

class AnonymousDemo {
   Polygon p1;
   String m1;
   
   public void foo1() {
      p1 = new Polygon("ss", "yy") {
          public String getVal() {
              return y;
           }
      };
      m1 = p1.getVal();
   }
   
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

/*$$$$$ AnonymousDemo#foo1(), 
   5,
   AnonymousDemo#foo1()$Polygon$1.<init>(), 
   AnonymousDemo#foo1()$Polygon$1#getVal(),
   Polygon#getVal(),
   AnonymousDemo#foo2()$Polygon$1#getVal(),
   AnonymousDemo#foo2()$Polygon$2#getVal(), 
 */

/*!!!!! AnonymousDemo#foo1(),  2, 424, 105, 542,11 */

/*$$$$$ AnonymousDemo#foo1()$Polygon$1.<init>(), 
  1, 
  Polygon#Polygon(String@@@String) 
*/

/*!!!!! AnonymousDemo#foo1()$Polygon$1.<init>(), 1, 448, 81 */


/*$$$$$ AnonymousDemo#foo2(), 
   6, 
   AnonymousDemo#foo2()$Polygon$1.<init>(), 
   AnonymousDemo#foo2()$Polygon$2.<init>(), 
   AnonymousDemo#foo2()$Polygon$2#getVal(),
   AnonymousDemo#foo1()$Polygon$1#getVal(),
   Polygon#getVal(),
   AnonymousDemo#foo2()$Polygon$1#getVal(), 
 */

/*!!!!! AnonymousDemo#foo2(), 3, 600, 99, 713,99,  826, 11 */

/*$$$$$ AnonymousDemo#foo2()$Polygon$1.<init>(), 
  1,
  Polygon#Polygon() 
*/

/*!!!!! AnonymousDemo#foo2()$Polygon$1.<init>(), 1, 614, 85 */

/*$$$$$ AnonymousDemo#foo2()$Polygon$2.<init>(), 
  1,
  Polygon#Polygon() 
*/

/*!!!!! AnonymousDemo#foo2()$Polygon$2.<init>(), 1, 727, 85*/
