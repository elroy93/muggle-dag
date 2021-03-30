package com.onemuggle.dagV2.dag;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;


@Getter
public class DagNodeProducer<Context> {

    private final IDagNode<Context> dagNode;
    private final boolean isAsync;
    private final ListeningExecutorService executionThreadPools;
    private final ListeningExecutorService monitorThreadPools;
    private final AtomicBoolean requested = new AtomicBoolean(false);
    private final List<? extends DagNodeMonitor<Context>> monitors;
    private ListenableFutureAdaptor<Object> future;
    @Setter
    private List<DagNodeProducer<Context>> fatherProducers;
    @Setter
    private List<DagNodeProducer<Context>> sonProducers;


    public DagNodeProducer(IDagNode<Context> dagNode,
                           boolean isAsync,
                           List<? extends DagNodeMonitor<Context>> monitors,
                           ListeningExecutorService executionThreadPools,
                           ListeningExecutorService monitorThreadPools) {
        this.dagNode = dagNode;
        this.isAsync = isAsync;
        this.executionThreadPools = executionThreadPools;
        this.monitorThreadPools = monitorThreadPools;
        this.monitors = monitors;
    }

    public ListenableFutureAdaptor<Object> sub(List<ListenableFutureAdaptor<Object>> fatherFutures, Context context){
        if (requested.compareAndSet(false, true)) {

        }
        return future;
    }

    public Object exe(Context context){

    }


    @SuppressWarnings("unchecked")
    public ListenableFutureAdaptor<Object> submit(List<ListenableFuture<Object>> fatherFutures, Context context) {
        if (requested.compareAndSet(false, true)) {

            ListenableFutureAdaptor<Object> futureAdaptor = ListenableFutureAdaptor.of(fatherFutures);
            // TODO 如果有异常
            Futures.transformAsync(futureAdaptor,inputs -> {
                for (Object input : inputs) {
                    if (input instanceof Future) {
                        ((Future) input).get();
                    }
                    doExecute(context);
                }
            } ,executionThreadPools);


            fatherFutures = Optional.ofNullable(fatherFutures).orElse(Collections.emptyList());
            ListenableFuture<List<Object>> fatherFuture = Futures.allAsList(fatherFutures);
            // 返回结果是异步的话,等到future执行完成,不开启新的线程 这应该是下一个节点执行的时候等待,目的是一个线程去等待多个资源ready TODO 整体的方案有问题,不能由父节点判断.
            if (isAsync) {
                future = ListenableFutureAdaptor.of(Futures.transform(fatherFuture, input -> {
                    try {
                        return ((Future) doExecute(context)).get();
                    } catch (Exception e) {
                        throw new RuntimeException(dagNode.getClass().getSimpleName() + " 节点执行失败", e);
                    }
                }, executionThreadPools));
            } else {
                if (CollectionUtils.isEmpty(fatherFutures)) {
                    future = ListenableFutureAdaptor.of(executionThreadPools.submit(() -> doExecute(context)));
                } else {
                    future = ListenableFutureAdaptor.of(Futures.transformAsync(fatherFuture, input -> Futures.immediateFuture(doExecute(context)), executionThreadPools));
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
