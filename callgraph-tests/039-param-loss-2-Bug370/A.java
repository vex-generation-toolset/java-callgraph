public class X {
  String str;
  public void bar() {
    str = "x"; 
  }
}

class Y extends X {
  public void bar() {
    str = "y"; 
  }
}

class Z extends Y {
  public void bar() {
    str = "z"; 
  }
}`

public class A {   
  public void foo1Control(X xx) {/*<<<<<21,2,23,2,callee,X:bar()V,Y:bar()V,Z:bar()V*/
    xx.bar();
  }
}