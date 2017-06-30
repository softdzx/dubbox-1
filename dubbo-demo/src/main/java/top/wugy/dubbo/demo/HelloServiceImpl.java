package top.wugy.dubbo.demo;

import com.alibaba.dubbo.rpc.RpcContext;

/**
 * Created by wugy on 2017/6/26 14:52
 */
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        System.out.println("Hello " + name + ", request from consumer: " +
                RpcContext.getContext().getRemoteAddress());
        return "Hello " + name + ", response form provider: " + RpcContext.getContext().getLocalAddress();
    }
}