package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.InvokerListener;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;
import com.google.common.base.Strings;


/**
 * AbstractConsumerConfig
 *
 * @author william.liangf
 * @see com.alibaba.dubbo.config.ReferenceConfig
 */
public abstract class AbstractReferenceConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = -2786526984373031126L;

    // ======== 引用缺省值，当引用属性未设置时使用该缺省值替代  ========

    /**
     * 检查服务提供者是否存在
     */
    protected Boolean check;

    /**
     * 是否加载时即刻初始化
     */
    protected Boolean init;

    /**
     * 是否使用泛接口
     */
    protected String generic;

    /**
     * 优先从JVM内获取引用实例
     */
    protected Boolean injvm;

    /**
     * lazy create connection
     */
    protected Boolean lazy;
    /**
     * reconnect
     */
    protected String reconnect;
    /**
     * sticky
     */
    protected Boolean sticky;

    /**
     * stub是否支持event事件. //TODO slove merge problem
     */
    protected Boolean stubevent;//= Constants.DEFAULT_STUB_EVENT;

    /**
     * 版本
     */
    protected String version;

    /**
     * 服务分组
     */
    protected String group;

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public Boolean isInit() {
        return init;
    }

    public void setInit(Boolean init) {
        this.init = init;
    }

    @Parameter(excluded = true)
    public Boolean isGeneric() {
        return ProtocolUtils.isGeneric(generic);
    }

    public void setGeneric(Boolean generic) {
        if (generic != null) {
            this.generic = generic.toString();
        }
    }

    public void setGeneric(String generic) {
        this.generic = generic;
    }

    public String getGeneric() {
        return generic;
    }

    /**
     * @return injvm indicator
     * @deprecated 通过scope进行判断，scope=local
     */
    @Deprecated
    public Boolean isInjvm() {
        return injvm;
    }

    /**
     * @param injvm injvm indicator
     * @deprecated 通过scope设置，scope=local表示使用injvm协议.
     */
    @Deprecated
    public void setInjvm(Boolean injvm) {
        this.injvm = injvm;
    }

    @Parameter(key = Constants.REFERENCE_FILTER_KEY, append = true)
    public String getFilter() {
        return super.getFilter();
    }

    @Parameter(key = Constants.INVOKER_LISTENER_KEY, append = true)
    public String getListener() {
        return super.getListener();
    }

    @Override
    public void setListener(String listener) {
        checkMultiExtension(InvokerListener.class, "listener", listener);
        super.setListener(listener);
    }

    @Parameter(key = Constants.LAZY_CONNECT_KEY)
    public Boolean getLazy() {
        return lazy;
    }

    public void setLazy(Boolean lazy) {
        this.lazy = lazy;
    }

    @Override
    public void setOnconnect(String onconnect) {
        if (!Strings.isNullOrEmpty(onconnect)) {
            this.stubevent = true;
        }
        super.setOnconnect(onconnect);
    }

    @Override
    public void setOndisconnect(String ondisconnect) {
        if (!Strings.isNullOrEmpty(ondisconnect)) {
            this.stubevent = true;
        }
        super.setOndisconnect(ondisconnect);
    }

    @Parameter(key = Constants.STUB_EVENT_KEY)
    public Boolean getStubevent() {
        return stubevent;
    }

    @Parameter(key = Constants.RECONNECT_KEY)
    public String getReconnect() {
        return reconnect;
    }

    public void setReconnect(String reconnect) {
        this.reconnect = reconnect;
    }

    @Parameter(key = Constants.CLUSTER_STICKY_KEY)
    public Boolean getSticky() {
        return sticky;
    }

    public void setSticky(Boolean sticky) {
        this.sticky = sticky;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        checkKey("version", version);
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        checkKey("group", group);
        this.group = group;
    }

}