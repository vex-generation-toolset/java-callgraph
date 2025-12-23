// Issue 23
// Static inner class that is in custom code

class Example6Custom {

    public static class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    void demo() {
        Pair<String, Integer> entry = new Pair<String, Integer>("A", 1);
    }
}

public class AnotherClass {
    public static void main(String[] args) {
        // Fully qualified name: Example6Custom.Pair
        Example6Custom.Pair<String, Integer> entry = new Example6Custom.Pair<String, Integer>("B", 2);

        System.out.println(entry);  // prints: (B, 2)
    }
}

/*$$$$$ Example6Custom#demo(), 1,
 		17,39, 1, Example6Custom.Pair#Pair(Object@@@Object)
*/


/*$$$$$ AnotherClass.main(String[]), 1,
		24,54, 1, Example6Custom.Pair#Pair(Object@@@Object)
*/
