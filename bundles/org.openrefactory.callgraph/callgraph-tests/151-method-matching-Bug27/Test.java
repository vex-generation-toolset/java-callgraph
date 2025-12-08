// Issue 27
// Method matching cases.
// Copy of points to test 800

public class Z {
    String aa;
    
    public String toString() {
        return super.toString()+"["+ toString("minute",0);
    }

    private String toString(String key, long bit) {
        return "";
    }
}

/*$$$$$ Z#toString(), 2,
   		9,38, 1, Z#toString(String@@@long),
   		9,16, 1, java.lang.Object#toString()
*/
