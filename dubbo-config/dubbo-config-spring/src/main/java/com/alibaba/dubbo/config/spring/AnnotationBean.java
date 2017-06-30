package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.annotation.DubboConsumer;
import com.alibaba.dubbo.config.annotation.DubboService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * AnnotationBean
 *
 * @author william.liangf
 */
@SuppressWarnings("Duplicates")
public class AnnotationBean extends AbstractConfig implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {

    private static final long serialVersionUID = -7582802454287589552L;

    private static final Logger logger = LoggerFactory.getLogger(Logger.class);

    private String annotationPackage;

    private String[] annotationPackages;

    private final Set<ServiceConfig<?>> serviceConfigs = new ConcurrentHashSet<>();

    private final ConcurrentMap<String, ReferenceBean<?>> referenceConfigs = Maps.newConcurrentMap();

    public String getPackage() {
        return annotationPackage;
    }

    public void setPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = Strings.isNullOrEmpty(annotationPackage) ? null
                : Constants.COMMA_SPLIT_PATTERN.split(annotationPackage);
    }

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        if (Strings.isNullOrEmpty(annotationPackage)) {
            return;
        }
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ReflectUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner");
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class})
                        .newInstance(beanFactory, true);
                // add filter
                Class<?> filterClass = ReflectUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter");
                Object filter = filterClass.getConstructor(Class.class).newInstance(DubboService.class);
                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter", ReflectUtils.forName("org.springframework.core.type.filter.TypeFilter"));
                addIncludeFilter.invoke(scanner, filter);
                // scan packages
                String[] packages = Constants.COMMA_SPLIT_PATTERN.split(annotationPackage);
                Method scan = scannerClass.getMethod("scan", String[].class);
                scan.invoke(scanner, new Object[]{packages});
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }

    public void destroy() throws Exception {
        for (ServiceConfig<?> serviceConfig : serviceConfigs) {
            try {
                serviceConfig.unexport();
            } catch (Throwable e) {
                LogHelper.warn(logger, e.getMessage(), e);
            }
        }
        for (ReferenceConfig<?> referenceConfig : referenceConfigs.values()) {
            try {
                referenceConfig.destroy();
            } catch (Throwable e) {
                LogHelper.warn(logger, e.getMessage(), e);
            }
        }
    }

    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        DubboService service = bean.getClass().getAnnotation(DubboService.class);
        if (service != null) {
            ServiceBean<Object> serviceConfig = new ServiceBean<>(service);
            if (void.class.equals(service.interfaceClass())
                    && "".equals(service.interfaceName())) {
                if (bean.getClass().getInterfaces().length > 0) {
                    serviceConfig.setInterface(bean.getClass().getInterfaces()[0]);
                } else {
                    throw new IllegalStateException("Failed to export remote service class " + bean.getClass().getName()
                            + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
                }
            }
            if (applicationContext != null) {
                serviceConfig.setApplicationContext(applicationContext);
                if (service.registry().length > 0) {
                    List<RegistryConfig> registryConfigs = Lists.newArrayList();
                    for (String registryId : service.registry()) {
                        if (!Strings.isNullOrEmpty(registryId)) {
                            registryConfigs.add(applicationContext.getBean(registryId, RegistryConfig.class));
                        }
                    }
                    serviceConfig.setRegistries(registryConfigs);
                }
                if (service.provider().length() > 0) {
                    serviceConfig.setProvider(applicationContext.getBean(service.provider(), ProviderConfig.class));
                }
                if (service.monitor().length() > 0) {
                    serviceConfig.setMonitor(applicationContext.getBean(service.monitor(), MonitorConfig.class));
                }
                if (service.application().length() > 0) {
                    serviceConfig.setApplication(applicationContext.getBean(service.application(), ApplicationConfig.class));
                }
                if (service.module().length() > 0) {
                    serviceConfig.setModule(applicationContext.getBean(service.module(), ModuleConfig.class));
                }
                if (service.provider().length() > 0) {
                    serviceConfig.setProvider(applicationContext.getBean(service.provider(), ProviderConfig.class));
                }
                if (service.protocol().length > 0) {
                    List<ProtocolConfig> protocolConfigs = Lists.newArrayList();
                    for (String protocolId : service.registry()) {
                        if (Strings.isNullOrEmpty(protocolId)) {
                            protocolConfigs.add(applicationContext.getBean(protocolId, ProtocolConfig.class));
                        }
                    }
                    serviceConfig.setProtocols(protocolConfigs);
                }
                try {
                    serviceConfig.afterPropertiesSet();
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            serviceConfig.setRef(bean);
            serviceConfigs.add(serviceConfig);
            serviceConfig.export();
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    DubboConsumer reference = method.getAnnotation(DubboConsumer.class);
                    if (reference != null) {
                        Object value = refer(reference, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean);
                        }
                    }
                } catch (Throwable e) {
                    LogHelper.error(logger, "Failed to init remote service reference at method " + name + " in class "
                            + bean.getClass().getName() + ", cause: " + e.getMessage(), e);
                }
            }
        }
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                DubboConsumer reference = field.getAnnotation(DubboConsumer.class);
                if (reference != null) {
                    Object value = refer(reference, field.getType());
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Throwable e) {
                LogHelper.error(logger, "Failed to init remote service reference at filed " + field.getName()
                        + " in class " + bean.getClass().getName() + ", cause: " + e.getMessage(), e);
            }
        }
        return bean;
    }

    private Object refer(DubboConsumer reference, Class<?> referenceClass) { //method.getParameterTypes()[0]
        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (referenceClass.isInterface()) {
            interfaceName = referenceClass.getName();
        } else {
            throw new IllegalStateException("The @Reference undefined interfaceClass or interfaceName, and the property type "
                    + referenceClass.getName() + " is not a interface.");
        }
        String key = reference.group() + "/" + interfaceName + ":" + reference.version();
        ReferenceBean<?> referenceConfig = referenceConfigs.get(key);
        if (referenceConfig == null) {
            referenceConfig = new ReferenceBean<>(reference);
            if (void.class.equals(reference.interfaceClass())
                    && "".equals(reference.interfaceName())
                    && referenceClass.isInterface()) {
                referenceConfig.setInterface(referenceClass);
            }
            if (applicationContext != null) {
                referenceConfig.setApplicationContext(applicationContext);
                if (reference.registry().length > 0) {
                    List<RegistryConfig> registryConfigs = new ArrayList<>();
                    for (String registryId : reference.registry()) {
                        if (!Strings.isNullOrEmpty(registryId)) {
                            registryConfigs.add(applicationContext.getBean(registryId, RegistryConfig.class));
                        }
                    }
                    referenceConfig.setRegistries(registryConfigs);
                }
                if (reference.consumer().length() > 0) {
                    referenceConfig.setConsumer(applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }
                if (reference.monitor().length() > 0) {
                    referenceConfig.setMonitor(applicationContext.getBean(reference.monitor(), MonitorConfig.class));
                }
                if (reference.application().length() > 0) {
                    referenceConfig.setApplication(applicationContext.getBean(reference.application(), ApplicationConfig.class));
                }
                if (reference.module().length() > 0) {
                    referenceConfig.setModule(applicationContext.getBean(reference.module(), ModuleConfig.class));
                }
                if (reference.consumer().length() > 0) {
                    referenceConfig.setConsumer(applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }
                try {
                    referenceConfig.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            referenceConfigs.putIfAbsent(key, referenceConfig);
            referenceConfig = referenceConfigs.get(key);
        }
        return referenceConfig.get();
    }

    private boolean isMatchPackage(Object bean) {
        if (CollectionUtils.isEmpty(annotationPackages)) {
            return true;
        }
        String beanClassName = bean.getClass().getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

}
