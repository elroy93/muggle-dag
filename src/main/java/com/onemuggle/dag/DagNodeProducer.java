package com.onemuggle.dag;

import com.google.common.base.Function;
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
    private final boolean isAsync;
    private final ListeningExecutorService executionThreadPools;
    private final ListeningExecutorService monitorThreadPools;
    private final AtomicBoolean requested = new AtomicBoolean(false);
    private final List<? extends DagNodeMonitor<Context>> monitors;
    private ListenableFuture<Object> future;


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

    public ListenableFuture<Object> submit(Context context) {
        return doGenerateFuture(context, false);
    }

    public ListenableFuture<Object> immediateFuture(Context context) {
        return doGenerateFuture(context, true);
    }

    private ListenableFuture<Object> doGenerateFuture(Context context, boolean immediate) {
        if (requested.compareAndSet(false, true)) {

            if (isAsync) {
                Object obj = doExecute(context);
                if (obj instanceof ListenableFuture) {
                    future = (ListenableFuture<Object>) obj;
                } else if (obj instanceof Future) {
                    future = JdkFutureAdapters.listenInPoolThread((Future) obj, executionThreadPools);
                } else {
                    throw new RuntimeException("异步流程操作只能回返future对象实例");
                }
            }else {
                if (immediate) {
                    future = Futures.immediateFuture(doExecute(context));
                }else {
                    future = executionThreadPools.submit(() -> doExecute(context));
                }
            }
        }
        // 添加listener,这个future执行完成之后调用executionAfter,而不是在doExecute中.
        // 因为doExecute可能会返回一个future对象.
        future.addListener(() -> monitors.forEach(monitor -> monitor.executionAfter(this, context)), monitorThreadPools);
        return future;
    }


    private Object doExecute(Context context) {
        monitors.forEach(monitor -> monitor.executionBefore(this, context));
        return dagNode.execute(context);
    }


    @SuppressWarnings("unchecked")
    public ListenableFuture<Object> submit(List<ListenableFuture<Object>> fatherFutures, Context context) {
        if (requested.compareAndSet(false, true)) {
            fatherFutures = Optional.ofNullable(fatherFutures).orElse(Collections.emptyList());
            ListenableFuture<List<Object>> fatherFuture = Futures.allAsList(fatherFutures);
            if (isAsync) {
                Function<Object, ListenableFuture<Object>> asyncExeDagFunction = input -> {
                    ListenableFuture<Object> retFuture;
                    Object dagNodeResult = doExecute(context);
                    if (dagNodeResult instanceof ListenableFuture) {
                        retFuture = (ListenableFuture<Object>) dagNodeResult;
                    } else if (dagNodeResult instanceof Future) {
                        retFuture = JdkFutureAdapters.listenInPoolThread((Future) dagNodeResult, executionThreadPools);
                    } else {
                        throw new RuntimeException("异步流程操作只能回返future对象实例");
                    }
                    return retFuture;
                };
                future = Futures.transform(fatherFuture, asyncExeDagFunction);
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

}
