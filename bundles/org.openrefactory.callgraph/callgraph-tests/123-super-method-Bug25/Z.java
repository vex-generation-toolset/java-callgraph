// Issue 25
// Subclass Overrides a Method and Calls super

import java.util.ArrayList;

public class MyList<E> extends ArrayList<E> {
    @Override
    public boolean add(E e) {
        System.out.println("Adding element: " + e);
        return super.add(e);  // calls ArrayList.add() from library
    }

    public static void main(String[] args) {
        MyList<String> list = new MyList<String>();
    }
}

/*$$$$$ MyList.main(String[]), 1,
  		14,31, 1, MyList.<init>(),
*/

/*$$$$$ MyList#add(Object), 1,
  		10,16, 1, java.util.ArrayList#add(Object)
*/
