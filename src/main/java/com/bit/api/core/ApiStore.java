package com.bit.api.core;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ApiStore {
    private ApplicationContext applicationContext;
    private Map<String, ApiRunnable> apiMap = new HashMap<>();

    public ApiStore(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext);
        this.applicationContext = applicationContext;
    }

    /**
     * 从Spring Bean中加载所有含有@ApiMapping注解的接口
     */
    public void loadApiFromSpringBeans() {
        //1. 获取所有ioc bean的名称
        //2. 遍历所有bean 中的方法，将方法中含有ApiMapping注解的方法加入apiMap中

        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanDefinitionNames) {
            Class<?> type = applicationContext.getType(beanName);
            for (Method method : type.getDeclaredMethods()) {
                APIMapping apiMapping = method.getAnnotation(APIMapping.class);
                if (apiMapping != null) {
                    addApiItem(apiMapping, beanName, method);
                }
            }

        }

    }

    private void addApiItem(APIMapping apiMapping, String beanName, Method method) {

        ApiRunnable apiRunnable = new ApiRunnable();
        apiRunnable.setApiMapping(apiMapping);
        apiRunnable.setTargetMethod(method);
        apiRunnable.setTargetName(beanName);
        apiRunnable.setApiName(apiMapping.value());

        apiMap.put(apiMapping.value(),apiRunnable);

    }

    /**
     * 根据ApiName查找ApiRunnable
     * @param apiName API名称，@APIMapping(value="com.bit.api.getUser")中的value
     * @return
     */
    public ApiRunnable findApiRunnable(String apiName) {
        return apiMap.get(apiName);
    }

    /**
     * 根据ApiName,version查找ApiRunnable
     * @param apiName  API名称，@APIMapping(value="com.bit.api.getUser")中的value
     * @param version  接口版本号
     * @return
     */
    public ApiRunnable findApiRunnable(String apiName, String version) {
        return apiMap.get(apiName + "_" + version);
    }

    public class ApiRunnable {
        private String apiName; //com.bit.api.getUser

        String targetName; //ioc bean 名称

        private Object target; //  UserServiceImpl 实例

        private Method targetMethod; // 目标方法

        private APIMapping apiMapping;

        public Object run(Object ...args) throws InvocationTargetException, IllegalAccessException {
            if (target == null) {
                target = applicationContext.getBean(targetName);
            }
            return targetMethod.invoke(target,args);
        }

        public Class[] getParamTypes() {
            return targetMethod.getParameterTypes();
        }

        public String getApiName() {
            return apiName;
        }

        public void setApiName(String apiName) {
            this.apiName = apiName;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public Object getTarget() {
            return target;
        }

        public void setTarget(Object target) {
            this.target = target;
        }

        public Method getTargetMethod() {
            return targetMethod;
        }

        public void setTargetMethod(Method targetMethod) {
            this.targetMethod = targetMethod;
        }

        public APIMapping getApiMapping() {
            return apiMapping;
        }

        public void setApiMapping(APIMapping apiMapping) {
            this.apiMapping = apiMapping;
        }
    }
}
