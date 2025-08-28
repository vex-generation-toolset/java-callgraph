// Issue 833

public class Z {
    public native String foo();/*<<<<< 4,4,4,29,caller,0*/   // 0 means no callees (introduced in 1374) 
    static {
        System.loadLibrary("nativezutils");
    }
    
    public void bar() {
        foo();
    }
}