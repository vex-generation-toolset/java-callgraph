// Issue 3
// Extended call graph entry for a this constructor call
// Performs constructor chaining

package org.openrefactory.test;

class Person {
    String name;
    int age;

    Person(String name) {
        this(name, 0); // reuse constructor with (String, int)
    }

    Person(int age) {
        this("Unknown", age); // reuse constructor with (String, int)
    }

    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public static void main(String[] args) {
        Person p1 = new Person("Alice");
        Person p2 = new Person(25);
        Person p3 = new Person("Bob", 30);
    }
}

/*$$$$$ org.openrefactory.test.Person.main(String[]), 3,
   		25,21, 1, org.openrefactory.test.Person#Person(String),
   		26,21, 1, org.openrefactory.test.Person#Person(int),
   		27,21, 1, org.openrefactory.test.Person#Person(String@@@int)
*/

/*$$$$$ org.openrefactory.test.Person#Person(String), 1,
   		12,9, 1, org.openrefactory.test.Person#Person(String@@@int),
*/

/*$$$$$ org.openrefactory.test.Person#Person(int), 1,
		16,9, 1, org.openrefactory.test.Person#Person(String@@@int),
*/
