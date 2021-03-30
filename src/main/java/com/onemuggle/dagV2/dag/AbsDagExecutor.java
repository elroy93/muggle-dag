package com.onemuggle.dagV2.dag;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.lang.Assert;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AbsDagExecutor<Context> implements IDagExecutor<Context> {

    // 初始化数据
    private final ListeningExecutorService executionThreadPools;
    private final ListeningExecutorService monitorThreadPools;
    private final List<IDagNode<Context>> dagNodes;
    private IDagNode<Context> lastNode = null; // 最后一个节点
    private IDagNode<Context> firstNode = null; // 初始节点

    private Map<IDagNode<Context>, List<IDagNode<Context>>> nodeFatherNodesMap;   // 节点和父节点列表;
    private Map<IDagNode<Context>, List<IDagNode<Context>>> nodeSonNodesMap;   // 节点和父节点列表;

    private Map<IDagNode<Context>, Boolean> isAsyncMap; // 节点是否是非阻塞节点的标识符
    private List<? extends DagNodeMonitor<Context>> monitors;

    /**
     * 构造器,每个图初始化一次. 不用每次提交任务的时候都初始化.
     * 如果工程只有一个图,可以使用单例模式构建.
     *
     * @param executionThreadPools 执行节点的线程池
     * @param monitorThreadPools   monitor使用的线程池
     * @param dagNodes             执行节点
     * @param monitors             节点监控
     */
    public AbsDagExecutor(ThreadPoolExecutor executionThreadPools,
                          ThreadPoolExecutor monitorThreadPools,
                          List<IDagNode<Context>> dagNodes,
                          List<? extends DagNodeMonitor<Context>> monitors) {
        this.dagNodes = dagNodes;
        this.monitors = Optional.ofNullable(monitors).orElseGet(Collections::emptyList);
        this.executionThreadPools = MoreExecutors.listeningDecorator(executionThreadPools);
        this.monitorThreadPools = MoreExecutors.listeningDecorator(monitorThreadPools);
        init();
    }

    /**
     * 初始化一个dag图
     */
    private void init() {
        Map<Class, IDagNode<Context>> clazzNodeMap = dagNodes.stream().collect(Collectors.toMap(Object::getClass, Function.identity()));

        nodeFatherNodesMap = Maps.newHashMap();
        isAsyncMap = Maps.newHashMap();
        // 获取注解上的依赖,寻找父节点
        for (IDagNode<Context> node : dagNodes) {
            RelyOn annotation = AnnotationUtil.getAnnotation(node.getClass(), RelyOn.class);
            if (annotation.isLastNode()) {
                Assert.isNull(lastNode, "只能存在一个lastNode=" + node.getClass().getName());
                lastNode = node;
            }
            // 查到父亲节点
            List<IDagNode<Context>> fatherNodes = Arrays.stream(annotation.value()).map(clazzNodeMap::get).collect(Collectors.toList());
            nodeFatherNodesMap.put(node, fatherNodes);
            // 给父亲节点添加子节点
            for (IDagNode<Context> fatherNode : fatherNodes) {
                List<IDagNode<Context>> sonNodes = nodeSonNodesMap.computeIfAbsent(fatherNode, k -> new ArrayList<>());
                sonNodes.add(node);
            }
        }

    }

    @Override
    public ListenableFuture<Object> submit(Context context) {
        // 生成producer,节点和producer的映射
        Map<IDagNode<Context>, DagNodeProducer<Context>> nodeProducerMap = Maps.newHashMap();
        for (IDagNode<Context> dagNode : dagNodes) {
            Boolean isAsync = isAsyncMap.get(dagNode);
            nodeProducerMap.put(dagNode, new DagNodeProducer<>(dagNode, isAsync, monitors, executionThreadPools, monitorThreadPools));
        }
        // 初始化进去父亲producer和子producer
        nodeProducerMap.forEach((node, producer) -> {
            List<IDagNode<Context>> fatherNodes = nodeFatherNodesMap.get(node);
            List<IDagNode<Context>> sonNodes = nodeSonNodesMap.get(node);
            producer.setFatherProducers(fatherNodes.stream().map(nodeProducerMap::get).collect(Collectors.toList()));
            producer.setSonProducers(sonNodes.stream().map(nodeProducerMap::get).collect(Collectors.toList()));
        });

        DagNodeProducer<Context> lastNodeProducer = nodeProducerMap.get(lastNode);

        DagGraph dagGraph = new DagGraph();

        // 构建dag图,并提交
        ListenableFuture<Object> futures = buildNodeFuture(context, lastNodeProducer);

        // 找到所有的头节点,触发头节点开始执行.每一个father执行完成,异步触发所有的子节点执行
        // 如果父亲节点的result是个future,子节点需要等待.
        // 如果子节点的父亲节点都已经执行完成,但是返回的result是未完成状态,则当前子节点需要等待结果执行完成.

        return futures;
    }

    private ListenableFuture<Object> buildNodeFuture(Context context,
                                                     DagNodeProducer<Context> currentNodeProducer) {

        if (currentNodeProducer.getFuture() != null) {
            return currentNodeProducer.getFuture();
        }

        Supplier<ListenableFuture<Object>> supplier = () -> {
            List<DagNodeProducer<Context>> fatherProducers = currentNodeProducer.getFatherProducers();
            List<ListenableFuture<Object>> fatherFutures = fatherProducers.stream()
                    .map(fatherProducer -> buildNodeFuture(context, fatherProducer))
                    .map(ListenableFuture::new)
                    .collect(Collectors.toList());
            return currentNodeProducer.submit(fatherFutures, context);
        };

        monitors.forEach(monitor -> monitor.buildFutureBefore(currentNodeProducer, context));
        ListenableFuture<Object> future = supplier.get();
        monitors.forEach(monitor -> monitor.buildFutureAfter(currentNodeProducer, context));

        return future;
    }


}
