package com.sky.interceptor;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.springframework.stereotype.Component;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义拦截器，实现公共字段自动填充
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
@Component
public class AutoFillInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取 MappedStatement
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];

        // 2. 获取 SQL 操作类型（INSERT / UPDATE）
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // 3. 如果不是 INSERT 或 UPDATE，直接放行
        if (sqlCommandType != SqlCommandType.INSERT && sqlCommandType != SqlCommandType.UPDATE) {
            return invocation.proceed();
        }

        // 4. 获取 Mapper 方法上的 @AutoFill 注解
        String mapperId = mappedStatement.getId(); // e.g. com.sky.mapper.CategoryMapper.insert
        String className = mapperId.substring(0, mapperId.lastIndexOf('.'));
        String methodName = mapperId.substring(mapperId.lastIndexOf('.') + 1);

        Class<?> mapperClass = Class.forName(className, false, mappedStatement.getConfiguration().getClass().getClassLoader());

        // 查找方法（注意：Mapper 接口没有方法重载，直接用方法名匹配即可）
        Method mapperMethod = findMapperMethod(mapperClass, methodName);
        if (mapperMethod == null) {
            return invocation.proceed();
        }

        AutoFill autoFill = mapperMethod.getAnnotation(AutoFill.class);
        if (autoFill == null) {
            return invocation.proceed();
        }

        // 5. 获取参数实体对象
        Object parameter = invocation.getArgs()[1];
        if (parameter == null) {
            return invocation.proceed();
        }

        // 6. 通过反射自动填充公共字段
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        if (sqlCommandType == SqlCommandType.INSERT) {
            // INSERT 操作：填充 4 个字段
            tryInvokeMethod(parameter, AutoFillConstant.SET_CREATE_TIME, now);
            tryInvokeMethod(parameter, AutoFillConstant.SET_UPDATE_TIME, now);
            tryInvokeMethod(parameter, AutoFillConstant.SET_CREATE_USER, currentId);
            tryInvokeMethod(parameter, AutoFillConstant.SET_UPDATE_USER, currentId);
            log.info("公共字段自动填充 - INSERT: createTime, updateTime, createUser, updateUser");
        } else if (sqlCommandType == SqlCommandType.UPDATE) {
            // UPDATE 操作：填充 2 个字段
            tryInvokeMethod(parameter, AutoFillConstant.SET_UPDATE_TIME, now);
            tryInvokeMethod(parameter, AutoFillConstant.SET_UPDATE_USER, currentId);
            log.info("公共字段自动填充 - UPDATE: updateTime, updateUser");
        }

        return invocation.proceed();
    }

    /**
     * 通过反射执行实体对象的 setter 方法
     */
    private void tryInvokeMethod(Object target, String methodName, Object value) {
        try {
            // 确定参数类型
            Class<?> paramType = value.getClass();
            // LocalDateTime 类型需要特殊处理（用 setter 的参数类型推断）
            Method setter = findSetterMethod(target.getClass(), methodName, paramType);
            if (setter != null) {
                setter.invoke(target, value);
            }
        } catch (Exception e) {
            log.warn("自动填充字段失败: method={}, target={}", methodName, target.getClass().getSimpleName(), e);
        }
    }

    /**
     * 查找目标类的 setter 方法
     */
    private Method findSetterMethod(Class<?> clazz, String methodName, Class<?> valueType) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                // 如果参数类型匹配，直接返回
                if (paramType.isAssignableFrom(valueType)) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * 从 Mapper 接口中查找指定名称的方法
     */
    private Method findMapperMethod(Class<?> mapperClass, String methodName) {
        // 先找当前接口中声明的方法
        for (Method method : mapperClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        // 如果没找到，从父接口中查找
        for (Method method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
