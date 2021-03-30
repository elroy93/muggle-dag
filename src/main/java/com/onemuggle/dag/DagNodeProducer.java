package com.onemuggle.dag;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;


@Getter
public class DagNodeProducer<Context> {

    private final IDagNode<Context> dagNode;
    private final List<IDagNode<Context>> fatherNodes;
    private final boolean isAsync;
    private final ListeningExecutorService executionThreadPools;
    private final ListeningExecutorService monitorThreadPools;
    private final AtomicBoolean requested = new AtomicBoolean(false);
    private final List<? extends DagNodeMonitor<Context>> monitors;
    private ListenableFuture<Object> future;

    private LinkedList<ListenableFuture<Object>> asyncResultList = new LinkedList<>();


    public DagNodeProducer(IDagNode<Context> dagNode,
                           List<IDagNode<Context>> fatherNodes,
                           boolean isAsync,
                           List<? extends DagNodeMonitor<Context>> monitors,
                           ListeningExecutorService executionThreadPools,
                           ListeningExecutorService monitorThreadPools) {
        this.dagNode = dagNode;
        this.fatherNodes = fatherNodes;
        this.isAsync = isAsync;
        this.executionThreadPools = executionThreadPools;
        this.monitorThreadPools = monitorThreadPools;
        this.monitors = monitors;
    }


    @SuppressWarnings("unchecked")
    public ListenableFuture<Object> submit(List<ListenableFuture<Object>> fatherFutures, Context context) {
        if (requested.compareAndSet(false, true)) {
            fatherFutures = Optional.ofNullable(fatherFutures).orElse(Collections.emptyList());
            ListenableFuture<List<Object>> fatherFuture = Futures.allAsList(fatherFutures);
            // 返回结果是异步的话,等到future执行完成,不开启新的线程 这应该是下一个节点执行的时候等待,目的是一个线程去等待多个资源ready TODO 整体的方案有问题,不能由父节点判断.
            if (isAsync) {
                future = Futures.transform(fatherFuture, input -> {
                    try {
                        return ((Future) doExecute(context)).get();
                    } catch (Exception e) {
                        throw new RuntimeException(dagNode.getClass().getSimpleName() + " 节点执行失败", e);
                    }
                }, executionThreadPools);
            } else {
                if (CollectionUtils.isEmpty(fatherFutures)) {
                    future = executionThreadPools.submit(() -> doExecute(context));
                } else {
                    future = Futures.transformAsync(fatherFuture, input -> Futures.immediateFuture(doExecute(context)), executionThreadPools);
                }
            }
            future.addListener(() -> monitors.forEach(monitor -> monitor.executionAfter(this, context)), monitorThreadPools);
        }
        return future;
    }

    private Object doExecute(Context context) {
        monitors.forEach(monitor -> monitor.executionBefore(this, context));
        return dagNode.execute(context);
    }


}
