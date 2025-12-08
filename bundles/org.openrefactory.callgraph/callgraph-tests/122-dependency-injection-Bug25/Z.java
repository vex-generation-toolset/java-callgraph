// Issue 25
// Dependency injection

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.Random;

public class Example12 {
    void demo() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        Random rand = ctx.getBean(Random.class);
    }
}

/*$$$$$ Example12#demo(), 2,
  		10,34, 1, org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext(Class),
  		11,23, 1, org.springframework.context.ApplicationContext#getBean(Class),
*/
