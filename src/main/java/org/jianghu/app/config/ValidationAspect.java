package org.jianghu.app.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import org.jianghu.app.common.*;
import org.jianghu.app.common.annotation.JHValidate;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
@Component
public class ValidationAspect {

    @Before("execution(* *.*(@org.jianghu.app.common.annotation.JHValidate (*), ..))")
    public void JHValid(JoinPoint joinPoint) throws Exception {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (annotation instanceof JHValidate) {
                    JHValidate jhValidate = (JHValidate) annotation;
                    String errorTip = jhValidate.errorTip();
                    if (StringUtils.isEmpty(jhValidate.value()) || !JSON.isValid(jhValidate.value(), JSONReader.Feature.AllowUnQuotedFieldNames)) {
                        throw new BizException(BizEnum.valid_schema_invalid);
                    }
                    JSONPathObject schemaObj = JsonUtil.parseObject(jhValidate.value(), JSONReader.Feature.AllowUnQuotedFieldNames);
                    JSONPathObject jsonData = JsonUtil.toJSON(arg);
                    ValidateUtil.validate(schemaObj, jsonData, errorTip);
                }
            }
        }
    }

}
