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
    boolean isLastNode() default false ;

    /**
     * 是否是非阻塞执行的.也就是需要返回future.
     * 且不使用传入的线程池进行提交,而是串行执行
     * @return
     */
    boolean isAync() default false;

    /**
     * 依赖的节点列表
     * @return
     */
    Class<?>[] value();

}
