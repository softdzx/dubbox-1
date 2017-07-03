package com.alibaba.dubbo.container.page.pages;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.page.Menu;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.google.common.collect.Lists;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Menu(name = "System", desc = "Show system environment information.", order = Integer.MAX_VALUE - 10000)
public class SystemPageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = Lists.newArrayList();
        List<String> row;

        row = Lists.newArrayList();
        row.add("Version");
        row.add(Version.getVersion(SystemPageHandler.class, "2.0.0"));
        rows.add(row);

        row = Lists.newArrayList();
        row.add("Host");
        String address = NetUtils.getLocalHost();
        row.add(NetUtils.getHostName(address) + "/" + address);
        rows.add(row);

        row = Lists.newArrayList();
        row.add("OS");
        row.add(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        rows.add(row);

        row = Lists.newArrayList();
        row.add("JVM");
        row.add(System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version") + ",<br/>"
                + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " "
                + System.getProperty("java.vm.info", ""));
        rows.add(row);

        row = Lists.newArrayList();
        row.add("CPU");
        row.add(System.getProperty("os.arch", "") + ", " + String.valueOf(Runtime.getRuntime().availableProcessors()) + " cores");
        rows.add(row);

        row = Lists.newArrayList();
        row.add("Locale");
        row.add(Locale.getDefault().toString() + "/" + System.getProperty("file.encoding"));
        rows.add(row);

        row = Lists.newArrayList();
        row.add("Uptime");
        row.add(formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()));
        rows.add(row);

        row = Lists.newArrayList();
        row.add("Time");
        row.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(new Date()));
        rows.add(row);

        return new Page("System", "System", new String[]{
                "Property", "Value"}, rows);
    }

    private static final long SECOND = 1000;

    private static final long MINUTE = 60 * SECOND;

    private static final long HOUR = 60 * MINUTE;

    private static final long DAY = 24 * HOUR;

    private String formatUptime(long uptime) {
        StringBuilder buf = new StringBuilder();
        if (uptime > DAY) {
            long days = (uptime - uptime % DAY) / DAY;
            buf.append(days);
            buf.append(" Days");
            uptime = uptime % DAY;
        }
        if (uptime > HOUR) {
            long hours = (uptime - uptime % HOUR) / HOUR;
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(hours);
            buf.append(" Hours");
            uptime = uptime % HOUR;
        }
        if (uptime > MINUTE) {
            long minutes = (uptime - uptime % MINUTE) / MINUTE;
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(minutes);
            buf.append(" Minutes");
            uptime = uptime % MINUTE;
        }
        if (uptime > SECOND) {
            long seconds = (uptime - uptime % SECOND) / SECOND;
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(seconds);
            buf.append(" Seconds");
            uptime = uptime % SECOND;
        }
        if (uptime > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(uptime);
            buf.append(" Milliseconds");
        }
        return buf.toString();
    }
}