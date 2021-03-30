package com.onemuggle.dag;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.lang.Assert;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.*;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AbsDagExecutor<Context> {

    // 初始化数据
    private final ListeningExecutorService executorService;
    private final List<IDagNode<Context>> dagNodes;
    private IDagNode<Context> lastNode = null; // 最后一个节点
    private Map<IDagNode<Context>, List<IDagNode<Context>>> nodeFatherMap;   // 节点和父节点列表;
    private Map<IDagNode<Context>, Boolean> isAsyncMap; // 节点是否是非阻塞节点的标识符
    private List<? extends DagNodeMonitor<Context>> monitors;

    public AbsDagExecutor(ThreadPoolExecutor threadPoolExecutor,
                          List<IDagNode<Context>> dagNodes,
                          List<? extends DagNodeMonitor<Context>> monitors) {
        this.dagNodes = dagNodes;
        this.monitors = Optional.ofNullable(monitors).orElseGet(Collections::emptyList);
        executorService = MoreExecutors.listeningDecorator(threadPoolExecutor);
        init();
    }

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
            isAsyncMap.put(node, annotation.isAync());
            List<IDagNode<Context>> fatherNodes = Arrays.stream(annotation.value()).map(clazzNodeMap::get).collect(Collectors.toList());
            nodeFatherMap.put(node, fatherNodes);
        }

    }


    public ListenableFuture<Object> submit(Context context) {
        // 生成producer
        Map<IDagNode<Context>, DagNodeProducer<Context>> nodeProducerMap = Maps.newHashMap();
        for (IDagNode<Context> dagNode : dagNodes) {
            List<IDagNode<Context>> fathers = nodeFatherMap.get(dagNode);
            Boolean isAsync = isAsyncMap.get(dagNode);
            nodeProducerMap.put(dagNode, new DagNodeProducer<>(dagNode, fathers, isAsync, executorService, monitors));
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
        return buildNodeFuture(context, lastNodeProducer, nodeFatherProducerMap);
    }

    private ListenableFuture<Object> buildNodeFuture(Context context,
                                                     DagNodeProducer<Context> currentNodeProducer,
                                                     Map<DagNodeProducer<Context>, List<DagNodeProducer<Context>>> nodeFatherProducerMap) {

        Supplier<ListenableFuture<Object>> supplier = () -> {
            List<DagNodeProducer<Context>> fatherProducers = nodeFatherProducerMap.get(currentNodeProducer);
            if (CollectionUtils.isEmpty(fatherProducers)) {
                return currentNodeProducer.submit(context);
            } else {
                List<ListenableFuture<Object>> fatherFutures = fatherProducers.stream()
                        .map(fatherProducer -> buildNodeFuture(context, fatherProducer, nodeFatherProducerMap))
                        .collect(Collectors.toList());
                return Futures.transformAsync(Futures.allAsList(fatherFutures),
                        input -> currentNodeProducer.immediateFuture(context),
                        executorService);
            }
        };


        monitors.forEach(monitor -> monitor.buildFutureBefore(currentNodeProducer, context));
        ListenableFuture<Object> future = supplier.get();
        monitors.forEach(monitor -> monitor.buildFutureAfter(currentNodeProducer, context));

        return future;
    }


}
