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

    /**
     * 运行时环境,执行之前
     *
     * @param executor
     */
    default void submitRuntimeBefore(AbsDagExecutor executor) {
    }

    /**
     * 运行时环境,执行之后
     *
     * @param executor
     */
    default void submitRuntimeAfter(AbsDagExecutor executor) {
    }
}
