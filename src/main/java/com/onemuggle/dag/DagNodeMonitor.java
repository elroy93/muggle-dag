package com.onemuggle.dag;

public interface DagNodeMonitor {

    /**
     * build节点之前
     */
    void buildFutureBefore(DagNodeProducer producer, Object context);

    /**
     * build节点之后
     */
    void buildFutureAfter(DagNodeProducer producer, Object context);

    /**
     * dagNode.execute执行之前
     */
    void executionBefore(DagNodeProducer producer, Object context);

    /**
     * dagNode.execute执行之后
     */
    void executionAfter(DagNodeProducer producer, Object context);
}
