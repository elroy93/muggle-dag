package com.onemuggle.dag;

public interface IDagNode<Context> {

    String SUCCESS = "success";
    String FAIL = "fail";

    /**
     * 执行dag节点
     * @param context   上下文
     * @return          success/fail
     */
    Object execute(Context context);
}
