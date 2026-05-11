package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点 + 注解生效的方法
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void AutoFillPointCut() {}

    @Before("AutoFillPointCut()")
    public void autoFillPointCut(JoinPoint joinPoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("开始进行公共字段填充......");

        //1.拿到这个 签名对象
        MethodSignature signature =(MethodSignature) joinPoint.getSignature();
        //2.通过签名对象拿到注解
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);
        //3.枚举类拿到 需要的操作类型
        OperationType operationType = annotation.value();

        //4.拿到方法参数对象
        Object[] args = joinPoint.getArgs();

        //判断拿到的 对象数组是否为空 空就 直接跳出方法
        if(args == null || args.length == 0){
            return;
        }

        //5.默认参数对象数组第一个为 赋值对象
        Object entity = args[0];

        //6.设置这个赋值数据
        LocalDateTime now = LocalDateTime.now(); //当前时间
        Long currentId = BaseContext.getCurrentId();

        //7.通过 操作类型来赋值

        if(operationType == OperationType.UPDATE){

            Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

            //反射赋值
            setUpdateTime.invoke(entity,now);
            setUpdateUser.invoke(entity,currentId);
        }else if(operationType == OperationType.INSERT){

            Method setCreatTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
            Method setCreatUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
            Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

            //反射赋值
            setCreatTime.invoke(entity,now);
            setCreatUser.invoke(entity,currentId);
            setUpdateTime.invoke(entity,now);
            setUpdateUser.invoke(entity,currentId);

        }


    }

}
