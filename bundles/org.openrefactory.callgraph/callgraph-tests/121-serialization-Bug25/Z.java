// Issue 25
// Serialization

import java.io.*;

public class Example11 {
    void demo() throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("obj.ser"))) {
            Object o = in.readObject();
        }
    }
}

/*$$$$$
  Example11#demo(), 3,
  java.io.ObjectInputStream#ObjectInputStream(FileInputStream),
  java.io.FileInputStream#FileInputStream(String),
  java.io.ObjectInputStream#readObject()
*/

/*!!!!! Example11#demo(), 3, 167, 30, 145, 53, 225, 15 */
