package com.onemuggle.dag.example;

import com.onemuggle.dag.IDagNode;
import com.onemuggle.dag.RelyOn;

import java.util.Map;

@RelyOn(value = A.class)
public class B implements IDagNode<Map<String,String>> {
    @Override
    public String execute(Map<String, String> map)  {
        map.put("B", "B");
        System.out.println(System.currentTimeMillis() + " "+Thread.currentThread().getName()  + " == exe == B [A 3]");

        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "B";
    }
}
