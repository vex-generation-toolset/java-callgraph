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
    default Z onTick(Z z) {
        z = new Z();
        z.aa = "mm";
        return z;
    }
}

interface IThird extends ISecond {}
 
class SecondImpl implements ISecond {

    @Override
    default Z onTick(Z z) {
        z = new Z();
        z.aa = "nn";
        return z;
    }
}


public final class A extends SecondImpl {
    
    public Z foo() {
        Z z = null;
        z = onTick(z);/*<<<<<29,4,33,4,caller,A:foo()QZ;*/
        return z;
    }
}






