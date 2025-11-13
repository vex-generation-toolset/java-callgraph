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

/*$$$$$ Z#toString(), 
   2,
   Z#toString(String@@@long),
   java.lang.Object#toString()
 */

/*!!!!! Z#toString(),  2, 152,16, 174,20 */
