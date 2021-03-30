package com.onemuggle.dag;

import lombok.Data;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultDagNodeMonitor implements DagNodeMonitor {

    @Getter
    private Map<DagNodeProducer, DefaultMonitorData> monitorDataMap = new LinkedHashMap<>();

    @Override
    public void buildFutureBefore(DagNodeProducer producer, Object context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setBuildFutureStartTime(System.currentTimeMillis());
    }

    @Override
    public void buildFutureAfter(DagNodeProducer producer, Object context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setBuildFutureEndTime(System.currentTimeMillis());
    }

    @Override
    public void executionBefore(DagNodeProducer producer, Object context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setExecutionStartTime(System.currentTimeMillis());
    }

    @Override
    public void executionAfter(DagNodeProducer producer, Object context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setExecutionEndTime(System.currentTimeMillis());
    }

    /**
     * 打印依赖关系
     *
     * @return
     */
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder("");
        String tmpl = "%-8s\t\t%-8s\t\t%-8s\n";
        sb.append(String.format(tmpl, "节点名称", "执行耗时(ms)", "父节点"));
        monitorDataMap.forEach((producer, monitorData) -> {
            sb.append(String.format(tmpl, producer.getDagNode().getClass().getSimpleName(),
                    monitorData.getExecutionEndTime() - monitorData.getExecutionStartTime(),
                    producer.getFatherNodes().stream().map(Object::getClass).map(clazz -> ((Class<?>) clazz).getSimpleName()).collect(Collectors.toList())
            ));
        });
        return sb.toString();
    }


    @Data
    class DefaultMonitorData {
        private DagNodeProducer dagNodeProducer;
        private long buildFutureStartTime;
        private long buildFutureEndTime;
        private long executionStartTime;
        private long executionEndTime;

        public DefaultMonitorData(DagNodeProducer dagNodeProducer) {
            this.dagNodeProducer = dagNodeProducer;
        }
    }

    private DefaultMonitorData getMonitor(DagNodeProducer dagNodeProducer) {
        return monitorDataMap.computeIfAbsent(dagNodeProducer, k -> new DefaultMonitorData(dagNodeProducer));
    }

}