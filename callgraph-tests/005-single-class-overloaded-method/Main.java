
public class Main {
    public static void main(String[] args) {
        A x = new A();
        int i = 10;
        while(i>0) {
            x = x.foo(new B()); 
            i--;
        }
        A y = new C();
        y.foo(x);
        MyClass mc = new MyClass();
        mc.bar();
        System.out.println("___finished___");
    }
}