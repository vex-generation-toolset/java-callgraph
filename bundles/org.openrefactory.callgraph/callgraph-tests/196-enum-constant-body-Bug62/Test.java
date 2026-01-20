// Issue 62
// Test for enum constant with a class body (polymorphism)

public enum Shape {
    CIRCLE {
        @Override
        public void draw() {
            System.out.println("Circle");
        }
    },
    SQUARE {
        @Override
        public void draw() {
            System.out.println("Square");
        }
    };

    public abstract void draw();

    public void render() {
        CIRCLE.draw();
    }
}

/*$$$$$ Shape#render(), 2,
20,5, 1, Shape.<staticinit>(),
21,9, 1, Shape$Shape$1#draw()
*/
