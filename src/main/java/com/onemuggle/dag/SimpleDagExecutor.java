package com.onemuggle.dag;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class SimpleDagExecutor<Context, Result> extends AbsDagExecutor<Context> {

    public SimpleDagExecutor(ThreadPoolExecutor threadPoolExecutor, List<IDagNode<Context>> dagNodes) {
        super(threadPoolExecutor, dagNodes);
    }


}
