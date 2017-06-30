package top.wugy.dubbo.demo;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wugy on 2017/6/26 15:58
 */
public class HelloProvider {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("hello-provider.xml");
        ctx.start();

        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}