package top.wugy.dubbo.demo;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wugy on 2017/6/26 15:03
 */
public class HelloConsumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("hello-consumer.xml");
        ctx.start();

        HelloService helloService = ctx.getBean("helloService", HelloService.class);
        System.out.println(helloService.sayHello("world"));

        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}