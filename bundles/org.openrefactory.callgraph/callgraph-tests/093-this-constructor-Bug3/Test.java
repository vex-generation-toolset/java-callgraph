// Issue 3
// Extended call graph entry for a this constructor call

package org.openrefactory.test;

class Car {
    String model;

    Car() {
        this.model = "Default Model";
        System.out.println("No-arg constructor");
    }

    Car(String model) {
        this();  // calls Car()
        this.model = model;
        System.out.println("Parameterized constructor");
    }

    public static void main(String[] args) {
        new Car("Tesla");
    }
}


/*$$$$$ org.openrefactory.test.Car.main(String[]), 
   1,
   org.openrefactory.test.Car#Car(String),
*/

/*$$$$$ org.openrefactory.test.Car#Car(String), 
   1,
   org.openrefactory.test.Car#Car(),
*/

/*!!!!! org.openrefactory.test.Car#Car(String), 1, 272,7 */
