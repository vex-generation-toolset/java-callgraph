// Issue 25
// Subclass of a Library Exception Class

public class MyException extends Exception {
    @Override
    public String getMessage() {
        return "Custom prefix: " + super.getMessage();  // calls Throwable.getMessage()
    }

    public MyException(String msg) {
        super(msg);
    }

    public static void main(String[] args) {
        try {
            throw new MyException("failure");
        } catch (MyException e) {
            System.out.println(e.getMessage());
        }
    }
}

/*$$$$$ MyException#getMessage(), 1,
  		7,36, 1, java.lang.Exception#getMessage(),
*/


/*$$$$$ MyException#MyException(String), 2,
  		11,9, 1, java.lang.Exception#Exception(String),
  		10,5, 1, MyException.<init>(),
*/

/*$$$$$ MyException.main(String[]), 2,
		16,19, 1, MyException#MyException(String),
		18,32, 1, MyException#getMessage(),
*/
