// Issue 415 
// Inspired from Baritone 

class M {
    String mm;
    public Z bazz() {
        mm = "nn";
        return new Z();
    }
}

class Z extends M {
    String aa;
    public Z() {
        aa = "ss";
    }
}

interface IAnother {
    default M bar() {
      return new M();  
    }
}

interface ITop {
    void onTick(Z z);
    Z bar();
}


interface ISecond extends ITop {

    @Override
    default void onTick(Z z) {}
    
    @Override
    default Z bar() {
        return new Z();
    }
}

interface IThird extends ISecond {}

interface IContext extends IThird {
    
}

class Context implements IContext, IAnother {
    Z z;
    public Context() {
        z = new Z();
        z.aa = "mm";
    }
}

        
public class ThirdImpl implements IThird {

    public final IContext ctx;

    protected ThirdImpl() {
    }
}


public final class A extends ThirdImpl {
    
    private Z foo() {
        ctx = new Context();
        Z feet = ctx.bar().bazz();/*<<<<<37,4,39,4,caller,A:foo()QZ;*/
        feet.aa = "zz";
        return feet;
    }
}