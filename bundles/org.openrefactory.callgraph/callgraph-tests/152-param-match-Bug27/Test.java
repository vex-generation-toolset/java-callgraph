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

/*$$$$$ Z#foo(), 1,
		42,18, 1, IterUtil.compose(Iterable@@@Object),
*/

/*$$$$$ Z#bar(), 1,
		47,18, 1, IterUtil.compose(Object@@@Iterable),
*/

/*$$$$$ Z#bazz(), 1,
		53,18, 1, IterUtil.compose(Iterable@@@Iterable),
*/

/*$$$$$ IterUtil.compose(Iterable@@@Object), 1,
		30,16, 1, ComposedIterable#ComposedIterable(Iterable@@@Object),
*/

/*$$$$$ IterUtil.compose(Object@@@Iterable), 1,
		26,16, 1, ComposedIterable#ComposedIterable(Object@@@Iterable),
*/

/*$$$$$ IterUtil.compose(Iterable@@@Iterable), 1,
		34,16, 1, ComposedIterable#ComposedIterable(Iterable@@@Iterable),
*/
