package com.onemuggle.dag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RelyOn {
    /**
     * 是否是最后一个节点
     */
    boolean isLastNode() default false;

    boolean isAsync() default false;


    /**
     * 依赖的节点列表
     *
     * @return
     */
    Class<?>[] value();

}
