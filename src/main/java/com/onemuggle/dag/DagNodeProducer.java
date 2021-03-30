package com.onemuggle.dag;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;


@Getter
public class DagNodeProducer<Context> {

    private final IDagNode<Context> dagNode;
    private final List<IDagNode<Context>> fatherNodes;
    private final ListeningExecutorService executionThreadPools;
    private final ListeningExecutorService monitorThreadPools;
    private final AtomicBoolean requested = new AtomicBoolean(false);
    private final List<? extends DagNodeMonitor> monitors;
    private final boolean isAsync ;
    private ListenableFuture<Object> future;


    public DagNodeProducer(IDagNode<Context> dagNode,
                           boolean isAsync ,
                           List<IDagNode<Context>> fatherNodes,
                           List<? extends DagNodeMonitor> monitors,
                           ListeningExecutorService executionThreadPools,
                           ListeningExecutorService monitorThreadPools) {
        this.dagNode = dagNode;
        this.fatherNodes = fatherNodes;
        this.executionThreadPools = executionThreadPools;
        this.monitorThreadPools = monitorThreadPools;
        this.monitors = monitors;
        this.isAsync = isAsync;
    }


    @SuppressWarnings("unchecked")
    public ListenableFuture<Object> submit(List<ListenableFuture<Object>> fatherFutures, Context context) {
        if (requested.compareAndSet(false, true)) {
            fatherFutures = Optional.ofNullable(fatherFutures).orElse(Collections.emptyList());
            ListenableFuture<List<Object>> fatherFuture = Futures.allAsList(fatherFutures);
            if (CollectionUtils.isEmpty(fatherFutures)) {
                future = executionThreadPools.submit(() -> doExecute(context));
            } else {
                future = Futures.transformAsync(fatherFuture, inputs -> {
                    for (Object input : inputs) {
                        if (input instanceof AsyncPair) {
                            Object futureResult = ((AsyncPair) input).getObject();
                            DagNodeProducer producer = ((AsyncPair) input).getProducer();
                            ListenableFuture fatherResultFuture = JdkFutureAdapters.listenInPoolThread((Future) futureResult, executionThreadPools);
                            fatherResultFuture.addListener(() -> monitors.forEach(monitor -> {
                                // 这个地方应该是上一个节点 而不是this
                                 monitor.executionAfter(producer, context);
                            }), monitorThreadPools);
                            fatherResultFuture.get();
                        }
                    }
                    return Futures.immediateFuture(doExecute(context));
                }, executionThreadPools);
            }
            if (!isAsync) {
                future.addListener(() -> monitors.forEach(monitor -> monitor.executionAfter(this, context)), monitorThreadPools);
            }else {
                // 如果是异步的,针对返回结果转一下
                future = Futures.transform(future, input -> AsyncPair.of(this, input));
            }
        }
        return future;
    }

    private Object doExecute(Context context) {
        monitors.forEach(monitor -> monitor.executionBefore(this, context));
        return dagNode.execute(context);
    }


    @Getter
    static class AsyncPair {
        private DagNodeProducer producer;
        private Object object;

        public static AsyncPair of(DagNodeProducer producer, Object obj){
            return new AsyncPair(producer, obj);
        }

        public AsyncPair(DagNodeProducer producer, Object object) {
            this.producer = producer;
            this.object = object;
        }
    }

}
