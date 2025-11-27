// Issue 52

interface Drawable {
    void draw();
}

record Rectangle(int width, int height) implements Drawable {
    public void draw() {
        Math.max(width, height);
    }
}

/*$$ Rectangle#draw(),
  1,
  java.lang.Math.max(int,int),
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
