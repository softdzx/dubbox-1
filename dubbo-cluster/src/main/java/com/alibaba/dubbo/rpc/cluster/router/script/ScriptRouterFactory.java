package com.alibaba.dubbo.rpc.cluster.router.script;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;

/**
 * ScriptRouterFactory
 * 
 * Script Router Factory用到的URL形如：
 * <ol>
 * <li> script://registyAddress?type=js&rule=xxxx
 * <li> script:///path/to/routerfile.js?type=js&rule=xxxx
 * <li> script://D:\path\to\routerfile.js?type=js&rule=xxxx
 * <li> script://C:/path/to/routerfile.js?type=js&rule=xxxx
 * </ol>
 * URL的Host一段包含的是Script Router内容的来源，Registry、File etc
 * 
 * @author william.liangf
 */
public class ScriptRouterFactory implements RouterFactory {
    
    public static final String NAME = "script";

    public Router getRouter(URL url) {
        return new ScriptRouter(url);
    }

}