// Issue 162
//

class Y {    
    private String name;
    private String val;
    
    Y(){
        name = "name";
        val = "value";
    }
    
    String getName() {
        return name;
    }
    
    String getVal() {
        return val;
    }
    
    String getNameAndVal() {
        return getName()+" "+getVal();
    }
    
    void setName(String n)  {
         name = n;
    }
    
    void setVal(String v) {
        val = v;
    }
}


public class X {    
    public String a;
    
    String foo(Y b) {
        return b.getNameAndVal();
    }
    
    void bar() {
        Y b = new Y();/*<<<<<21,4,23,4,caller,X:foo(QY;)QString;*/
        b.setName("Escober");
        b.setVal("10");
        a = foo(b);
    }
}
