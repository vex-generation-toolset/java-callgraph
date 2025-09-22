// Issue 3
// Extended call graph entry for a this constructor call
// Performs constructor chaining

package org.openrefactory.test;

class Box {
    int length, width, height;

    Box() {
        this(1);  // calls Box(int)
    }

    Box(int size) {
        this(size, size, size);  // calls Box(int,int,int)
    }

    Box(int l, int w, int h) {
        this.length = l;
        this.width = w;
        this.height = h;
        System.out.println("Box created: " + l + "x" + w + "x" + h);
    }

    public static void main(String[] args) {
        new Box(); // chains through all
    }
}


/*$$$$$ org.openrefactory.test.Box.main(String[]), 
   1,
   org.openrefactory.test.Box#Box(),
*/

/*$$$$$ org.openrefactory.test.Box#Box(), 
   1,
   org.openrefactory.test.Box#Box(int),
*/

/*$$$$$ org.openrefactory.test.Box#Box(int), 
    1,
    org.openrefactory.test.Box#Box(int@@@int@@@int),
*/
