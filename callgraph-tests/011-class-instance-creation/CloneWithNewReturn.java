import java.util.List;

public class CloneWithNewReturn {
    int x, p;
    int z;
    Fist<Z> nn;
    
    public CloneWithNewReturn(int y, int z, Fist<Z> n) {/*<<<<<8,4,12,4,caller,CloneWithNewReturn:clone()QCloneWithNewReturn;*/
        x = z;
        this.z = y;
        nn = n;
    }
    
    @Override
    protected CloneWithNewReturn clone() { 
        int i = 6;
        int j = 100;
        CloneWithNewReturn copy = new CloneWithNewReturn(i++, 20 + j--, new Fist<Z>());
        return copy;
    }
}
class Z {
    String ss;
}

class Fist<T> {
    T[] item;
}
