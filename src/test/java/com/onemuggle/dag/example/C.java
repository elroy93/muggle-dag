package com.onemuggle.dag.example;

import com.onemuggle.dag.IDagNode;
import com.onemuggle.dag.RelyOn;

import java.util.Map;

@RelyOn(value = {B.class, B2.class}, isLastNode = true)
public class C implements IDagNode<Map<String, String>> {
    @Override
    public String execute(Map<String, String> map) {
        map.put("C", "C");

        System.out.println(System.currentTimeMillis() + " " + Thread.currentThread().getName() + " == exe == C [B,B2 1]");

        try {
            Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "C";
    }
}
