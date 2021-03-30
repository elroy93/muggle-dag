package com.onemuggle.dag.example;

import cn.hutool.core.thread.NamedThreadFactory;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.onemuggle.dag.DefaultDagNodeMonitor;
import com.onemuggle.dag.SimpleDagExecutor;
import com.onemuggle.dag.IDagNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DagTestExample {


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        long start = System.currentTimeMillis();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50,
                50,
                5,
                TimeUnit.MINUTES,
                new LinkedBlockingDeque<>(1000),
                new NamedThreadFactory("修然", false));

        List<IDagNode<Map<String, String>>> nodes = Lists.newArrayList(A.class, A2.class, B.class, B2.class, C.class)
                .stream()
                .map(clazz -> instanceClazz(clazz))
                .collect(Collectors.toList());

        Map<String, String> ctx = new LinkedHashMap<>();

        List<DefaultDagNodeMonitor<Map<String, String>>> monitors = Lists.newArrayList(new DefaultDagNodeMonitor());

        ListenableFuture<Object> future = new SimpleDagExecutor<>(threadPoolExecutor, nodes, monitors).submit(ctx);

        Object str = future.get();


        System.out.println("============= end 耗时 " + (System.currentTimeMillis() - start) + " ms  ============ ");
        System.out.println(str);

        System.out.println("==============================");
        monitors.forEach(monitor -> System.out.println(monitor.prettyPrint()));

        System.exit(9);
    }

    private static IDagNode<Map<String, String>> instanceClazz(Class<? extends IDagNode> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


}
