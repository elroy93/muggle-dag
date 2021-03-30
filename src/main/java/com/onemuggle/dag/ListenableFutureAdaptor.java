package com.onemuggle.dag;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ListenableFutureAdaptor implements ListenableFuture {

    public ListenableFutureAdaptor(ListenableFuture future) {
        this.future = future;
    }

    private ListenableFuture future;

    @Override
    public void addListener(Runnable listener, Executor executor) {
        future.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
}
