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
        if (requested.compareAndSet(false, true)) {
            if (isAsync) {
                Object obj = execute(context);
                if (obj instanceof ListenableFuture) {
                    return (ListenableFuture<Object>) obj;
                } else if (obj instanceof Future) {
                    return JdkFutureAdapters.listenInPoolThread((Future)obj, executorService);
                }else {
                    throw new RuntimeException("异步流程操作只能回返future对象实例");
                }
            }else {
                return executorService.submit(() -> execute(context));
            }
        }
        return future;
    }

    public Object execute(Context context){
        return dagNode.execute(context);
    }
}
