// Issue 27
// Method matching cases.
// Copy of points to test j52

public class ComposedIterable<T> extends Iterable<T> {
  String str;

  /** The result contains {@code i1}'s elements followed by {@code i2}'s elements. */
  public ComposedIterable(Iterable<? extends T> i1, Iterable<? extends T> i2) {
    str = "ii";
  }

  /** The result contains {@code v1} followed by {@code i2}'s elements */
  public ComposedIterable(T v1, Iterable<? extends T> i2) {
      str = "ti";
  }

  /** The result contains {@code i1}'s elements followed by {@code v2} */
  public ComposedIterable(Iterable<? extends T> i1, T v2) {
      str = "it";
  }
}
  
public final class IterUtil {
    public static <T> ComposedIterable<T> compose(T first, Iterable<? extends T> rest) {
        return new ComposedIterable<T>(first, rest);
      }
    
    public static <T> ComposedIterable<T> compose(Iterable<? extends T> rest, T last) {
        return new ComposedIterable<T>(rest, last);
      }
    
    public static <T> ComposedIterable<T> compose(Iterable<? extends T> i1, Iterable<? extends T> i2) {
        return new ComposedIterable<T>(i1, i2);  
      }
}

public class Z {
    Iterable<File> result;
    void foo () {
        File buildDir;
        result = IterUtil.compose(result, buildDir);  
    }
    
    void bar () {
        File buildDir;
        result = IterUtil.compose(buildDir, result);   
    }
    
    
    void bazz () {
        File buildDir;
        result = IterUtil.compose(result, result); 
    }
}

/*!!!!! Z#foo(), 1, 1251, 34 */

/*$$$$$ Z#foo(), 
   1,
   IterUtil.compose(Iterable@@@T),
 */

/*$$$$$ Z#bar(), 
1,
IterUtil.compose(T@@@Iterable),
*/

/*$$$$$ Z#bazz(), 
1,
IterUtil.compose(Iterable@@@Iterable),
*/


/*$$$$$ IterUtil.compose(Iterable@@@T), 
1,
ComposedIterable#ComposedIterable(Iterable@@@T),
*/

/*$$$$$ IterUtil.compose(T@@@Iterable), 
1,
ComposedIterable#ComposedIterable(T@@@Iterable),
*/

/*$$$$$ IterUtil.compose(Iterable@@@Iterable), 
1,
ComposedIterable#ComposedIterable(Iterable@@@Iterable),
*/

/*!!!!! Z#bar(), 1, 1358,34 */
/*!!!!! Z#bazz(), 1, 1472,32 */
/*!!!!! IterUtil.compose(Iterable@@@T), 1, 934,35 */
/*!!!!! IterUtil.compose(T@@@Iterable), 1, 780,36 */
/*!!!!! IterUtil.compose(Iterable@@@Iterable), 1,1103,31 */
