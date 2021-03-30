package com.onemuggle.dag;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class SimpleDagExecutor<Context> extends AbsDagExecutor<Context> {

    /**
     *
     * @param executionThreadPools      执行节点的线程池
     * @param monitorThreadPools        monitor使用的线程池
     * @param dagNodes                  执行节点
     * @param monitors                  节点监控
     */
    public SimpleDagExecutor(ThreadPoolExecutor executionThreadPools,
                             ThreadPoolExecutor monitorThreadPools,
                             List<IDagNode<Context>> dagNodes,
                             DagMonitorFactory monitorFactory) {
        super(executionThreadPools, monitorThreadPools, dagNodes, monitorFactory);
    }


}
