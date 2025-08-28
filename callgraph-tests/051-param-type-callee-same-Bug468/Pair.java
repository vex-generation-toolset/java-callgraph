
public class Pair<T, U> {
    T fst;
    U snd;
    public static <X, Y> Pair<X, Y> of(X fst, Y snd)
    {
        return new Pair<X, Y>(fst, snd);
    }
    
    public Pair(T fst, U snd) {
        this.fst = fst;
        this.snd = snd;
    }
    
    public T getFst() {
        return fst; 
    }
    
    public U getSnd() {
        return snd;
    }
    
    public Pair<T, U> get(){
        return of(getFst(), getSnd());
    }
}

class Z {
    String ss;
    public Z() {
        
    }
}
class Foo {
    Pair<Z, Z> pair = new Pair<Z,Z>(new Z(), new Z());
    public void foo() {

        pair.getFst();
        pair.getSnd();/*<<<<< 23,4,25,4,callee,Pair:getFst()QT;,Pair:getSnd()QU;,Pair:of(QX;QY;)QPair<QX;QY;>;*/
    }
}

