// Issue 374
// Add support for this() constructor

public class X 
{ 
    Z p;
    
    X() { 
      this(new Z()); 
    } 
  
    X(Z x) { 
       this(x, new Z());/*<<<<<12,5,14,5,caller,X:X()V*/
    } 
  
    X(Z x, Z y) { /*<<<<<16,5,18,5,caller,X:X(QZ;)V*/
        p = x; 
    } 
  
    public static X foo() { 
        return new X(); 
    } 
} 

class Z {
    String a;
}