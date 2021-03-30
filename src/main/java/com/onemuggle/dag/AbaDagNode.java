package com.onemuggle.dag;

@Deprecated
public  abstract class AbaDagNode<Context> implements IDagNode<Context>{

    public abstract Object exe(Context context);

}
