package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.annotation.DubboService;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * ServiceFactoryBean
 *
 * @author william.liangf
 */
@SuppressWarnings("Duplicates")
public class ServiceBean<T> extends ServiceConfig<T> implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener, BeanNameAware {

    private static final long serialVersionUID = 213195494150089726L;

    private static transient ApplicationContext SPRING_CONTEXT;

    private transient ApplicationContext applicationContext;

    private transient String beanName;

    private transient boolean supportedApplicationListener;

    public ServiceBean() {
        super();
    }

    public ServiceBean(DubboService service) {
        super(service);
    }

    public static ApplicationContext getSpringContext() {
        return SPRING_CONTEXT;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
        if (applicationContext != null) {
            SPRING_CONTEXT = applicationContext;
            try {
                Method method = applicationContext.getClass().getMethod("addApplicationListener", ApplicationListener.class); // 兼容Spring2.0.1
                method.invoke(applicationContext, this);
                supportedApplicationListener = true;
            } catch (Throwable t) {
                if (applicationContext instanceof AbstractApplicationContext) {
                    try {
                        Method method = AbstractApplicationContext.class.getDeclaredMethod("addListener", ApplicationListener.class); // 兼容Spring2.0.1
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        method.invoke(applicationContext, this);
                        supportedApplicationListener = true;
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }

    public void setBeanName(String name) {
        this.beanName = name;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (ContextRefreshedEvent.class.getName().equals(event.getClass().getName())) {
            if (isDelay() && !isExported() && !isUnexported()) {
                LogHelper.info(logger, "The service ready on spring started. service: " + getInterface());
                export();
            }
        }
    }

    private boolean isDelay() {
        Integer delay = getDelay();
        ProviderConfig provider = getProvider();
        if (delay == null && provider != null) {
            delay = provider.getDelay();
        }
        return supportedApplicationListener && (delay == null || delay == -1);
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    public void afterPropertiesSet() throws Exception {
        if (getProvider() == null) {
            Map<String, ProviderConfig> providerConfigMap = applicationContext == null ? null :
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class, false, false);
            if (!CollectionUtils.isEmpty(providerConfigMap)) {
                Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null :
                        BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
                if (CollectionUtils.isEmpty(protocolConfigMap)
                        && providerConfigMap.size() > 1) { // 兼容旧版本
                    List<ProviderConfig> providerConfigs = Lists.newArrayList();
                    for (ProviderConfig config : providerConfigMap.values()) {
                        if (config.isDefault() != null && config.isDefault()) {
                            providerConfigs.add(config);
                        }
                    }
                    if (providerConfigs.size() > 0) {
                        setProviders(providerConfigs);
                    }
                } else {
                    ProviderConfig providerConfig = null;
                    for (ProviderConfig config : providerConfigMap.values()) {
                        if (config.isDefault() == null || config.isDefault()) {
                            if (providerConfig != null) {
                                throw new IllegalStateException("Duplicate provider configs: " + providerConfig + " and " + config);
                            }
                            providerConfig = config;
                        }
                    }
                    if (providerConfig != null) {
                        setProvider(providerConfig);
                    }
                }
            }
        }
        if (getApplication() == null
                && (getProvider() == null || getProvider().getApplication() == null)) {
            Map<String, ApplicationConfig> applicationConfigMap = applicationContext == null ? null :
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class, false, false);
            if (!CollectionUtils.isEmpty(applicationConfigMap)) {
                ApplicationConfig applicationConfig = null;
                for (ApplicationConfig config : applicationConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault()) {
                        if (applicationConfig != null) {
                            throw new IllegalStateException("Duplicate application configs: " + applicationConfig + " and " + config);
                        }
                        applicationConfig = config;
                    }
                }
                if (applicationConfig != null) {
                    setApplication(applicationConfig);
                }
            }
        }
        if (getModule() == null
                && (getProvider() == null || getProvider().getModule() == null)) {
            Map<String, ModuleConfig> moduleConfigMap = applicationContext == null ? null :
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class, false, false);
            if (!CollectionUtils.isEmpty(moduleConfigMap)) {
                ModuleConfig moduleConfig = null;
                for (ModuleConfig config : moduleConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault()) {
                        if (moduleConfig != null) {
                            throw new IllegalStateException("Duplicate module configs: " + moduleConfig + " and " + config);
                        }
                        moduleConfig = config;
                    }
                }
                if (moduleConfig != null) {
                    setModule(moduleConfig);
                }
            }
        }
        if (CollectionUtils.isEmpty(getRegistries())
                && (getProvider() == null || CollectionUtils.isEmpty(getProvider().getRegistries()))
                && (getApplication() == null || CollectionUtils.isEmpty(getApplication().getRegistries()))) {
            Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null :
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
            if (!CollectionUtils.isEmpty(registryConfigMap)) {
                List<RegistryConfig> registryConfigs = Lists.newArrayList();
                for (RegistryConfig config : registryConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault()) {
                        registryConfigs.add(config);
                    }
                }
                if (registryConfigs.size() > 0) {
                    super.setRegistries(registryConfigs);
                }
            }
        }
        if (getMonitor() == null
                && (getProvider() == null || getProvider().getMonitor() == null)
                && (getApplication() == null || getApplication().getMonitor() == null)) {
            Map<String, MonitorConfig> monitorConfigMap = applicationContext == null ? null :
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class, false, false);
            if (!CollectionUtils.isEmpty(monitorConfigMap)) {
                MonitorConfig monitorConfig = null;
                for (MonitorConfig config : monitorConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault()) {
                        if (monitorConfig != null) {
                            throw new IllegalStateException("Duplicate monitor configs: " + monitorConfig + " and " + config);
                        }
                        monitorConfig = config;
                    }
                }
                if (monitorConfig != null) {
                    setMonitor(monitorConfig);
                }
            }
        }
        if (CollectionUtils.isEmpty(getProtocols()) &&
                (getProvider() == null || CollectionUtils.isEmpty(getProvider().getProtocols()))) {
            Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null :
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
            if (!CollectionUtils.isEmpty(protocolConfigMap)) {
                List<ProtocolConfig> protocolConfigs = Lists.newArrayList();
                for (ProtocolConfig config : protocolConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault()) {
                        protocolConfigs.add(config);
                    }
                }
                if (protocolConfigs.size() > 0) {
                    super.setProtocols(protocolConfigs);
                }
            }
        }
        if (Strings.isNullOrEmpty(getPath())) {
            if (!Strings.isNullOrEmpty(beanName) && !Strings.isNullOrEmpty(getInterface())
                    && beanName.startsWith(getInterface())) {
                setPath(beanName);
            }
        }
        if (!isDelay()) {
            export();
        }
    }

    public void destroy() throws Exception {
        unexport();
    }

}