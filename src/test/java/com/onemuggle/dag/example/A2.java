package com.onemuggle.dag.example;

import com.onemuggle.dag.IDagNode;
import com.onemuggle.dag.RelyOn;

import java.util.Map;

@RelyOn(value = {})
public class A2 implements IDagNode<Map<String, String>> {
    @Override
    public String execute(Map<String, String> map) {
        map.put("A2", "A2");
        System.out.println(System.currentTimeMillis() + " " + Thread.currentThread().getName() + " == exe == A2");

        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(System.currentTimeMillis() + " " + Thread.currentThread().getName() + " == 结束 == A2");

        return "A2";
    }
}
