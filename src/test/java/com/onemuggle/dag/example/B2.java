package com.onemuggle.dag.example;

import com.onemuggle.dag.IDagNode;
import com.onemuggle.dag.RelyOn;

import java.util.Map;

@RelyOn(value = {A.class, A2.class})

public class B2 implements IDagNode<Map<String, String>> {
    @Override
    public String execute(Map<String, String> map) {
        map.put("B2", "B2");
        System.out.println(System.currentTimeMillis() + " " + Thread.currentThread().getName() + " == exe == B2 [A A2 4]");

        try {
            Thread.sleep(4 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "B2";
    }
}
