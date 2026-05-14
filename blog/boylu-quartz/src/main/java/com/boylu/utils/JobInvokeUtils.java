package com.boylu.utils;

import com.boylu.entity.SysJob;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility methods for invoking scheduled job targets.
 *
 * @author: boylu
 * @date 2021/12/8
 */
public class JobInvokeUtils {
    public static void invokeMethod(SysJob job) throws Exception {
        String invokeTarget = job.getInvokeTarget();
        String beanName = getBeanName(invokeTarget);
        String methodName = getMethodName(invokeTarget);
        List<Object[]> methodParams = getMethodParams(invokeTarget);

        if (!isValidClassName(beanName)) {
            Object bean = SpringUtil.getBean(beanName);
            invokeMethod(bean, methodName, methodParams);
        } else {
            Object bean = Class.forName(beanName).getDeclaredConstructor().newInstance();
            invokeMethod(bean, methodName, methodParams);
        }
    }

    private static void invokeMethod(Object bean, String methodName, List<Object[]> methodParams)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (methodParams != null && !methodParams.isEmpty()) {
            Method method = bean.getClass().getDeclaredMethod(methodName, getMethodParamsType(methodParams));
            method.invoke(bean, getMethodParamsValue(methodParams));
        } else {
            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.invoke(bean);
        }
    }

    public static boolean isValidClassName(String invokeTarget) {
        return StringUtils.countMatches(invokeTarget, ".") > 1;
    }

    public static String getBeanName(String invokeTarget) {
        String beanName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringBeforeLast(beanName, ".");
    }

    public static String getMethodName(String invokeTarget) {
        String methodName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringAfterLast(methodName, ".");
    }

    public static List<Object[]> getMethodParams(String invokeTarget) {
        String methodStr = StringUtils.substringBetween(invokeTarget, "(", ")");
        if (StringUtils.isBlank(methodStr)) {
            return null;
        }

        String[] methodParams = methodStr.split(",(?=(?:[^']*'[^']*')*[^']*$)");
        List<Object[]> params = new LinkedList<>();
        for (String methodParam : methodParams) {
            String str = StringUtils.trimToEmpty(methodParam);
            if (StringUtils.startsWith(str, "'") && StringUtils.endsWith(str, "'")) {
                params.add(new Object[]{StringUtils.substring(str, 1, str.length() - 1), String.class});
            } else if (StringUtils.equalsAnyIgnoreCase(str, "true", "false")) {
                params.add(new Object[]{Boolean.valueOf(str), Boolean.class});
            } else if (StringUtils.endsWithIgnoreCase(str, "L")) {
                params.add(new Object[]{Long.valueOf(StringUtils.removeEndIgnoreCase(str, "L")), Long.class});
            } else if (StringUtils.endsWithIgnoreCase(str, "D")) {
                params.add(new Object[]{Double.valueOf(StringUtils.removeEndIgnoreCase(str, "D")), Double.class});
            } else {
                params.add(new Object[]{Integer.valueOf(str), Integer.class});
            }
        }
        return params;
    }

    public static Class<?>[] getMethodParamsType(List<Object[]> methodParams) {
        Class<?>[] classes = new Class<?>[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams) {
            classes[index] = (Class<?>) os[1];
            index++;
        }
        return classes;
    }

    public static Object[] getMethodParamsValue(List<Object[]> methodParams) {
        Object[] values = new Object[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams) {
            values[index] = os[0];
            index++;
        }
        return values;
    }
}
