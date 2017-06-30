package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.support.Parameter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * ModuleConfig
 *
 * @author william.liangf
 */
public class ModuleConfig extends AbstractConfig {

    private static final long serialVersionUID = 5508512956753757169L;

    /**
     * 模块名称
     */
    private String name;

    /**
     * 模块版本
     */
    private String version;

    /**
     * 应用负责人
     */
    private String owner;

    /**
     * 组织名(BU或部门)
     */
    private String organization;

    /**
     * 注册中心
     */
    private List<RegistryConfig> registries;

    /**
     * 服务监控
     */
    private MonitorConfig monitor;

    /**
     * 是否为缺省
     */
    private Boolean isDefault;

    public ModuleConfig() {
    }

    public ModuleConfig(String name) {
        setName(name);
    }

    @Parameter(key = "module", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (Strings.isNullOrEmpty(id)) {
            id = name;
        }
    }

    @Parameter(key = "module.version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        checkName("owner", owner);
        this.owner = owner;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        checkName("organization", organization);
        this.organization = organization;
    }

    public RegistryConfig getRegistry() {
        return CollectionUtils.isEmpty(registries) ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = Lists.newArrayListWithCapacity(1);
        registries.add(registry);
        this.registries = registries;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
    }

    public void setMonitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

}