class X{
    String a;
}

public class SimpleBase {
  public String b;
  public String c;
  X y;
  public SimpleBase() {/*<<<<<9,2,12,2,caller,SimpleBase2:SimpleBase2()V*/
    this(y);
    c = "world";
  }
  public SimpleBase(X x) {/*<<<<<13,2,15,2,caller,SimpleBase:SimpleBase()V*/
    b = "hello";
  }        
}

class SimpleBase2 extends SimpleBase {
    public SimpleBase2() {
        super();
        y.a = "PP";
      }
}

class Test {
    SimpleBase2 c;
    public void foo() {
        c = new SimpleBase2();        
    }
}
