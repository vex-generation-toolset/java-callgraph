// Issue 415 
// Inspired from Baritone 

class Z {
    String aa;
}


interface ITop {
    void onTick(Z z);
}


interface ISecond extends ITop {

    @Override
    default void onTick(Z z) {}
}

public interface IThird extends ISecond {}

public interface IContext {
    default Z bar() {
        return new Z();
    }
}

        
public class ThirdImpl implements IThird {

    public final IContext ctx;

    protected ThirdImpl() {
    }
}


public final class A extends ThirdImpl {
    
    private Z foo() {
        Z feet = ctx.bar();/*<<<<<23,4,25,4,caller,A:foo()QZ;*/
        feet.aa = "ss";
        return feet;
    }
}






