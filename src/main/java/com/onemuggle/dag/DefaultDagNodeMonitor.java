package com.onemuggle.dag;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultDagNodeMonitor implements DagNodeMonitor {

    private Map<String, List<String>> nodeFatherNodeNameMap = Maps.newHashMap();

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

    @Override
    public void submitRuntimeBefore(AbsDagExecutor executor) {
        IDagNode lastNode = executor.getLastNode();
        Map<IDagNode<?>, List<IDagNode<?>>> nodeFatherMap = executor.getNodeFatherMap();
        nodeFatherMap.forEach((node, fathers) -> {
            nodeFatherNodeNameMap.put(node.getClass().getSimpleName(), fathers.stream().map(Object::getClass).map(Class::getSimpleName).collect(Collectors.toList()));
        });
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

    /**
     * http://www.plantuml.com/plantuml/
     * 画状态转移图的地方那个
     * @return
     */
    public String toPlatUML(){
        StringBuilder sb = new StringBuilder("");
        sb.append("@startuml\n scale 300 width\n");

        monitorDataMap.forEach((producer,monitorData) ->{
            String currentNodeName = producer.getDagNode().getClass().getSimpleName();
            String executeTime = monitorData.getExecutionEndTime() - monitorData.getExecutionStartTime() + " ms\n";
            List<String> fathers = (List)producer.getFatherNodes().stream().map(Object::getClass).map(clazz -> ((Class<?>) clazz).getSimpleName()).collect(Collectors.toList());
            for (String father : fathers) {
                sb.append(father).append(" --> ").append(currentNodeName).append("\n");
            }
            if (fathers.isEmpty()) {
                sb.append("[*]").append(" --> ").append(currentNodeName).append("\n");
            }
            sb.append(currentNodeName).append(" : ").append(executeTime).append("\n");
        });

        sb.append("@enduml");
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