package com.onemuggle.dagV2.dag;

import com.google.common.util.concurrent.ListenableFuture;

public interface IDagExecutor<Context> {

    ListenableFuture<Object> submit(Context context);
}
