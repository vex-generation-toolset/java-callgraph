// Issue 52
// Test for record implementing an interface that calls a library method

interface Drawable {
    void draw();
}

record Rectangle(int width, int height) implements Drawable {
    @Override
    public void draw() {
        String upper = "hello".toUpperCase();
    }
}

/*$$ Rectangle#draw(), 1,
  		11,24, 1, java.lang.String#toUpperCase()
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
