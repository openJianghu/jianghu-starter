package org.jianghu.app.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JHValidate {
    String errorTip() default "请求actionData不符合规范";
    String value();
}
