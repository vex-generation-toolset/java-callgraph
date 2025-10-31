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

/*$$$$$
  Example12#demo(), 2,
  org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext(Class<Config>),
  org.springframework.context.ApplicationContext#getBean(Class<Random>),
*/
