package com.onemuggle.dag;

import com.google.common.util.concurrent.ListenableFuture;

public interface IDagExecutor<Context> {

    DagResult submit(Context context);
}
