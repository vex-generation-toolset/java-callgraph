// Issue 415 
// Inspired from Baritone 

class Z {
    String aa;
}


interface ITop {
    Z onTick(Z z);
}

interface IFourth extends ITop {
    @Override
    default Z onTick(Z z) {
        z = new Z();
        z.aa = "hh";
        return z;
    }
}

interface ISecond extends ITop {

    @Override
    default Z onTick(Z z) {
        z = new Z();
        z.aa = "mm";
        return z;
    }
}

interface IThird extends ISecond {}
 
class ThirdImpl implements IThird {

    protected ThirdImpl() {
    }
}


public class A extends ThirdImpl {
    
    public Z foo() {
        Z z = null;
        z = onTick(z);/*<<<<<25,4,29,4,caller,A:foo()QZ;*/
        return z;
    }
}


