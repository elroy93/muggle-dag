package com.onemuggle.dag;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class SimpleDagExecutor<Context> extends AbsDagExecutor<Context> {

    public SimpleDagExecutor(ThreadPoolExecutor executionThreadPools,
                             ThreadPoolExecutor monitorThreadPools,
                             List<IDagNode<Context>> dagNodes,
                             List<? extends DagNodeMonitor<Context>> monitors) {
        super(executionThreadPools, monitorThreadPools, dagNodes, monitors);
    }


}
