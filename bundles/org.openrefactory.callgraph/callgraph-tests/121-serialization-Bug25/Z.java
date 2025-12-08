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

/*$$$$$ Example11#demo(), 3,
  		8,37, 1, java.io.ObjectInputStream#ObjectInputStream(FileInputStream),
  		8,59, 1, java.io.FileInputStream#FileInputStream(String),
  		9,24, 1, java.io.ObjectInputStream#readObject()
*/
