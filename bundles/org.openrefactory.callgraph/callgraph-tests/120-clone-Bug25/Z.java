// Issue 25
// Cloning

import java.util.ArrayList;

public class Example10 {
    void demo() throws CloneNotSupportedException {
        ArrayList<String> list1 = new ArrayList<>();
        ArrayList<String> list2 = (ArrayList<String>) list1.clone();
    }
}

/*$$$$$
  Example10#demo(), 3,
  java.util.ArrayList#ArrayList(),
  java.util.ArrayList.<init>(),
  java.util.ArrayList#clone()
*/
