// Issue 25
// Subclass Overrides a Method from an Abstract Library Class

import java.util.AbstractList;

public class MyList2 extends AbstractList<String> {
    @Override
    public String get(int index) {
        return "Value@" + index;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean add(String e) {
        System.out.println("Custom add");
        return super.add(e);  // calls AbstractList.add()
    }

    public static void main(String[] args) {
        MyList2 list = new MyList2();
        list.add("Hi");
    }
}

/*$$$$$
  MyList2.main(String[]), 2,
  MyList2.<init>(),
  MyList2#add(String)
*/

/*$$$$$
 MyList2#add(String), 1,
 java.util.AbstractList#add(String)
 */ 

/*$$$$$
MyList2#size(), 0,
*/

/*$$$$$
MyList2#get(int), 0,
*/
