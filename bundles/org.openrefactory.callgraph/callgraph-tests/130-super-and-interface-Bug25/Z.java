// Issue 25
// When the Library Superclass Implements an Interface

import java.util.AbstractList;
import java.util.List;

public class MyList3 extends AbstractList<String> implements List<String> {
    @Override
    public String get(int index) { return "X"; }
    @Override
    public int size() { return 1; }

    @Override
    public boolean add(String e) {
        System.out.println("Custom add with List contract");
        return super.add(e);  // calls AbstractList.add()
    }
}

/*$$$$$ MyList3#add(String), 1,
  		16,16, 1, java.util.AbstractList#add(String),
*/

/*$$$$$ MyList3#size(), 0 */

/*$$$$$ MyList3#get(int), 0 */
