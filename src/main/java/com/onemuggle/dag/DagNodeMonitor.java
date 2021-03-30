package com.onemuggle.dag;

public interface DagNodeMonitor<Context> {

    /**
     * build节点之前
     */
    void buildFutureBefore(DagNodeProducer<Context> producer);

    /**
     * build节点之后
     */
    void buildFutureAfter(DagNodeProducer<Context> producer, Context context);

    /**
     * dagNode.execute执行之前
     */
    void executionBefore(DagNodeProducer<Context> producer, Context context);

    /**
     * dagNode.execute执行之后
     */
    void executionAfter(DagNodeProducer<Context> producer, Context context);
}
