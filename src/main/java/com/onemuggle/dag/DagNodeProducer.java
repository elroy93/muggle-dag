package com.onemuggle.dag;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;


public class DagNodeProducer<Context> {

    @Getter
    private IDagNode<Context> dagNode;
    @Getter
    private List<IDagNode<Context>> fatherNodes;
    private boolean isAsync;
    private ListeningExecutorService executorService;
    private AtomicBoolean requested = new AtomicBoolean(false);
    private ListenableFuture<Object> future;

    public DagNodeProducer(IDagNode<Context> dagNode, List<IDagNode<Context>> fatherNodes, boolean isAsync, ListeningExecutorService executorService) {
        this.dagNode = dagNode;
        this.fatherNodes = fatherNodes;
        this.isAsync = isAsync;
        this.executorService = executorService;
    }

    public ListenableFuture<Object> submit(Context context){
        return doGenerateFuture(context, false);
    }

    public ListenableFuture<Object> immediateFuture(Context context){
        return doGenerateFuture(context, true);
    }

    private ListenableFuture<Object> doGenerateFuture(Context context,boolean immediate){
        if (requested.compareAndSet(false, true)) {
            if (immediate) {
                future = Futures.immediateFuture(doExecute(context));
            }else {
                if (isAsync) {
                    Object obj = doExecute(context);
                    if (obj instanceof ListenableFuture) {
                        future = (ListenableFuture<Object>) obj;
                    } else if (obj instanceof Future) {
                        future =  JdkFutureAdapters.listenInPoolThread((Future)obj, executorService);
                    }else {
                        throw new RuntimeException("异步流程操作只能回返future对象实例");
                    }
                }else {
                    future = executorService.submit(() -> doExecute(context));
                }
            }
        }
        return future;
    }


    private Object doExecute(Context context){
        return dagNode.execute(context);
    }
}
