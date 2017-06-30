package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.annotation.DubboConsumer;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.config.support.Parameter;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;

/**
 * ReferenceFactoryBean
 *
 * @author william.liangf
 */
public class ReferenceBean<T> extends ReferenceConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {

    private static final long serialVersionUID = 213195494150089726L;

    private transient ApplicationContext applicationContext;

    public ReferenceBean() {
        super();
    }

    public ReferenceBean(DubboConsumer reference) {
        super(reference);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }

    public Object getObject() throws Exception {
        return get();
    }

    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Parameter(excluded = true)
    public boolean isSingleton() {
        return true;
    }

    @SuppressWarnings({"unchecked"})
    public void afterPropertiesSet() throws Exception {
        ConsumerConfig consumer = getConsumer();
        if (consumer == null) {
            Map<String, ConsumerConfig> consumerConfigMap = applicationContext == null ? null :
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class, false, false);
            if (!CollectionUtils.isEmpty(consumerConfigMap)) {
                ConsumerConfig consumerConfig = null;
                for (ConsumerConfig config : consumerConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault()) {
                        if (consumerConfig != null) {
                            throw new IllegalStateException("Duplicate consumer configs: " + consumerConfig + " and " + config);
                        }
                        consumerConfig = config;
                    }
                }
                if (consumerConfig != null) {
                    setConsumer(consumerConfig);
                }
            }
        }
        if (getApplication() == null
                && (consumer == null || consumer.getApplication() == null)) {
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
                && (consumer == null || consumer.getModule() == null)) {
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
                && (consumer == null || CollectionUtils.isEmpty(consumer.getRegistries()))
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
                && (consumer == null || consumer.getMonitor() == null)
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
        Boolean b = isInit();
        if (b == null && consumer != null) {
            b = consumer.isInit();
        }
        if (b != null && b) {
            getObject();
        }
    }

}