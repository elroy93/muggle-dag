package com.onemuggle.dagV2.dag;

import cn.hutool.core.util.BooleanUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ListenableFutureAdaptor<V> implements ListenableFuture<List<V>> {

    public ListenableFutureAdaptor(ListenableFuture<V> future) {
        this.futures = Lists.newArrayList(future);
    }

    public ListenableFutureAdaptor(List<? extends ListenableFuture<V>> futures) {
        this.futures = futures;
    }

    public static <R> ListenableFutureAdaptor<R> of(ListenableFuture<R> future) {
        return new ListenableFutureAdaptor<>(future);
    }

    public static <R> ListenableFutureAdaptor<R> of(List<? extends ListenableFuture<R>> futures) {
        return new ListenableFutureAdaptor<>(futures);
    }

    private List<? extends ListenableFuture<V>> futures;


    /******************************************/

    @Override
    public void addListener(Runnable listener, Executor executor) {
        futures.forEach(future -> future.addListener(listener, executor));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futures.stream().map(future -> future.cancel(mayInterruptIfRunning)).allMatch(BooleanUtil::isTrue);
    }

    @Override
    public boolean isCancelled() {
        return futures.stream().map(Future::isCancelled).anyMatch(BooleanUtil::isTrue);
    }

    @Override
    public boolean isDone() {
        return futures.stream().map(ListenableFuture::isDone).anyMatch(BooleanUtil::isTrue);
    }

    @Override
    public List<V> get() throws InterruptedException, ExecutionException {
        return futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public List<V> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return futures.stream().map(f -> {
            try {
                return f.get(timeout, unit);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public boolean isMulti(){
        return futures.size() > 1;
    }
}
