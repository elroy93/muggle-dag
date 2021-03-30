package com.onemuggle.dag.example;

import com.onemuggle.dag.IDagNode;
import com.onemuggle.dag.RelyOn;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RelyOn(value = {A.class, A2.class})

public class B2_Async implements IDagNode<Map<String, String>> {
    public static ExecutorService service = Executors.newFixedThreadPool(2);

    @Override
    public Future execute(Map<String, String> map) {
        map.put("B2", "B2");
        System.out.println(System.currentTimeMillis() + " " + Thread.currentThread().getName() + " == exe == B2_Async");
        Future<?> future = service.submit(() -> {
            try {
                Thread.sleep(3 * 1000);
                System.out.println(System.currentTimeMillis() + " " + Thread.currentThread().getName() + " == 结束 == B2_Async");

            } catch (InterruptedException e) {
                System.out.println(System.currentTimeMillis() + " " + Thread.currentThread().getName() + " == 异常 == B2_Async");

            }
        });
        return future;
    }
}
