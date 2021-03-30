package com.onemuggle.dag;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultDagNodeMonitor<Context> implements DagNodeMonitor<Context>{

    @Getter
    private Map<DagNodeProducer, DefaultMonitorData> monitorDataMap = new LinkedHashMap<>();

    @Override
    public void buildFutureBefore(DagNodeProducer<Context> producer, Context context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setBuildFutureStartTime(System.currentTimeMillis());
    }

    @Override
    public void buildFutureAfter(DagNodeProducer<Context> producer, Context context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setBuildFutureEndTime(System.currentTimeMillis());
    }

    @Override
    public void executionBefore(DagNodeProducer<Context> producer, Context context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setExecutionStartTime(System.currentTimeMillis());
    }

    @Override
    public void executionAfter(DagNodeProducer<Context> producer, Context context) {
        DefaultMonitorData monitor = getMonitor(producer);
        monitor.setExecutionEndTime(System.currentTimeMillis());
    }

    /**
     * 打印依赖关系
     * @return
     */
    public String prettyPrint(){
        StringBuilder sb = new StringBuilder("");
        sb.append("节点名称 \t 执行耗时(ms) \t 父节点 \n");
        String tmpl = "{} \t {} \t {} \n";
        monitorDataMap.forEach((producer,monitorData) ->{
            sb.append(StrUtil.format(tmpl, producer.getDagNode().getClass().getSimpleName(),
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