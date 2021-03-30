package com.onemuggle.dag.example;

import com.onemuggle.dag.IDagNode;
import com.onemuggle.dag.RelyOn;

import java.util.Map;

@RelyOn(value = {})
public class A implements IDagNode<Map<String,String>> {
    @Override
    public String execute(Map<String, String> map)  {
        map.put("A", "A");
        System.out.println(System.currentTimeMillis() + " "+Thread.currentThread().getName()  + " == exe == A [1]");
        try {
            Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "A";
    }
}
