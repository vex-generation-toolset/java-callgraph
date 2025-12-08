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

/*$$$$$ AnonymousDemo#foo1(), 2,
   		28,12, 1, AnonymousDemo#foo1()$Polygon$1.<init>(), 
   		33,12, 4, Polygon#getVal(), AnonymousDemo#foo1()$Polygon$1#getVal(), AnonymousDemo#foo2()$Polygon$1#getVal(), AnonymousDemo#foo2()$Polygon$2#getVal(),
*/

/*$$$$$ AnonymousDemo#foo1()$Polygon$1.<init>(), 1, 
		28,36, 1, Polygon#Polygon(String@@@String) 
*/

/*$$$$$ AnonymousDemo#foo2(), 3, 
   		37,13, 1, AnonymousDemo#foo2()$Polygon$1.<init>(), 
   		42,13, 1, AnonymousDemo#foo2()$Polygon$2.<init>(), 
   		47,13, 4, Polygon#getVal(), AnonymousDemo#foo2()$Polygon$2#getVal(), AnonymousDemo#foo1()$Polygon$1#getVal(), AnonymousDemo#foo2()$Polygon$1#getVal(), 
*/

/*$$$$$ AnonymousDemo#foo2()$Polygon$1.<init>(), 1,
  		37,27, 1, Polygon#Polygon()
*/

/*$$$$$ AnonymousDemo#foo2()$Polygon$2.<init>(), 1,
		42,27, 1, Polygon#Polygon() 
*/
