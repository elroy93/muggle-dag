package com.onemuggle.dag;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.lang.Assert;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class AbsDagExecutor<Context> implements IDagExecutor<Context> {

    // 初始化数据
    private final ListeningExecutorService executionThreadPools;
    private final ListeningExecutorService monitorThreadPools;
    private final List<IDagNode<Context>> dagNodes;
    private IDagNode<Context> lastNode = null; // 最后一个节点
    private Map<IDagNode<Context>, List<IDagNode<Context>>> nodeFatherMap;   // 节点和父节点列表;
    private Map<IDagNode<Context>, Boolean> isAsyncMap;   // 节点和父节点列表;
    private DagMonitorFactory monitorFactory;

    /**
     * 构造器,每个图初始化一次. 不用每次提交任务的时候都初始化.
     * 如果工程只有一个图,可以使用单例模式构建.
     *
     * @param executionThreadPools 执行节点的线程池
     * @param monitorThreadPools   monitor使用的线程池
     * @param dagNodes             执行节点
     * @param monitorFactory       节点监控
     */
    public AbsDagExecutor(ThreadPoolExecutor executionThreadPools,
                          ThreadPoolExecutor monitorThreadPools,
                          List<IDagNode<Context>> dagNodes,
                          DagMonitorFactory monitorFactory) {
        this.dagNodes = dagNodes;
        this.monitorFactory = Optional.ofNullable(monitorFactory).orElse(() -> Lists.newArrayList(new DefaultDagNodeMonitor()));
        this.executionThreadPools = MoreExecutors.listeningDecorator(executionThreadPools);
        this.monitorThreadPools = MoreExecutors.listeningDecorator(monitorThreadPools);
        init();
    }

    /**
     * 初始化一个dag图
     */
    private void init() {
        Map<Class, IDagNode<Context>> clazzNodeMap = dagNodes.stream().collect(Collectors.toMap(Object::getClass, Function.identity()));

        nodeFatherMap = Maps.newHashMap();
        isAsyncMap = Maps.newHashMap();
        // 获取注解上的依赖,寻找父节点
        for (IDagNode<Context> node : dagNodes) {
            RelyOn annotation = AnnotationUtil.getAnnotation(node.getClass(), RelyOn.class);
            if (annotation.isLastNode()) {
                Assert.isNull(lastNode, "只能存在一个lastNode=" + node.getClass().getName());
                lastNode = node;
            }
            isAsyncMap.put(node, annotation.isAsync());
            List<IDagNode<Context>> fatherNodes = Arrays.stream(annotation.value()).map(clazzNodeMap::get).collect(Collectors.toList());
            nodeFatherMap.put(node, fatherNodes);
        }

    }

    @Override
    public DagResult submit(Context context) {
        List<DagNodeMonitor> monitors = monitorFactory.getMonitors();
        // 生成producer
        Map<IDagNode<Context>, DagNodeProducer<Context>> nodeProducerMap = Maps.newHashMap();
        for (IDagNode<Context> dagNode : dagNodes) {
            List<IDagNode<Context>> fathers = nodeFatherMap.get(dagNode);
            nodeProducerMap.put(dagNode, new DagNodeProducer<>(dagNode, isAsyncMap.get(dagNode), fathers, monitors, executionThreadPools, monitorThreadPools));
        }
        Map<DagNodeProducer<Context>, List<DagNodeProducer<Context>>> nodeFatherProducerMap = Maps.newHashMap();

        nodeProducerMap.forEach((currentNode, currentProducer) -> {
            List<DagNodeProducer<Context>> fatherProducers = Optional.ofNullable(currentProducer.getFatherNodes())
                    .orElseGet(Lists::newArrayList)
                    .stream().map(nodeProducerMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            nodeFatherProducerMap.put(currentProducer, fatherProducers);
        });

        DagNodeProducer<Context> lastNodeProducer = nodeProducerMap.get(lastNode);

        // 构建dag图,并提交
        ListenableFuture<Object> future = buildNodeFuture(context, monitors, lastNodeProducer, nodeFatherProducerMap);
        return DagResult.builder().future(future)
                .monitors(monitors)
                .build();
    }

    private ListenableFuture<Object> buildNodeFuture(Context context,
                                                     List<DagNodeMonitor> monitors,
                                                     DagNodeProducer<Context> currentNodeProducer,
                                                     Map<DagNodeProducer<Context>, List<DagNodeProducer<Context>>> nodeFatherProducerMap) {

        if (currentNodeProducer.getFuture() != null) {
            return currentNodeProducer.getFuture();
        }

        Supplier<ListenableFuture<Object>> supplier = () -> {
            List<DagNodeProducer<Context>> fatherProducers = nodeFatherProducerMap.get(currentNodeProducer);
            List<ListenableFuture<Object>> fatherFutures = fatherProducers.stream()
                    .map(fatherProducer -> buildNodeFuture(context, monitors, fatherProducer, nodeFatherProducerMap))
                    .collect(Collectors.toList());
            return currentNodeProducer.submit(fatherFutures, context);
        };

        monitors.forEach(monitor -> monitor.buildFutureBefore(currentNodeProducer, context));
        ListenableFuture<Object> future = supplier.get();
        monitors.forEach(monitor -> monitor.buildFutureAfter(currentNodeProducer, context));

        return future;
    }


}
