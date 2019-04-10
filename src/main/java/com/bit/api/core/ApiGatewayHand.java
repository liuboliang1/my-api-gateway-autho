package com.bit.api.core;

import com.bit.api.common.ApiException;
import com.bit.api.common.Md5Util;
import com.bit.api.common.UtilJson;
import com.bit.api.core.impl.TokenServiceImpl;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ApiGatewayHand implements InitializingBean, ApplicationContextAware {

    private Logger logger = LoggerFactory.getLogger(ApiGatewayHand.class);

    private static final String METHOD = "method";
    private static final String PARAMS = "params";

    //该类可以获取参数的名称
    private final ParameterNameDiscoverer parameterUtil;

    private ApiStore apiStore;

    //TODO
    private TokenService tokenService = new TokenServiceImpl();

    public ApiGatewayHand() {
        parameterUtil = new LocalVariableTableParameterNameDiscoverer();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        apiStore.loadApiFromSpringBeans();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        apiStore = new ApiStore(applicationContext);
    }


    public void handle(HttpServletRequest request, HttpServletResponse response) {
        String method = request.getParameter(METHOD);
        String params = request.getParameter(PARAMS);
        ApiStore.ApiRunnable apiRunnable = null;
        Object result = null;

        try {
            //1.校验系统参数
            apiRunnable = sysParamsValdate(request);

            //2.构建ApiRequest
            ApiRequest apiRequest = buildApiRequest(request);
            if (apiRequest.getAccessToken() != null) {
                signCheck(apiRequest);
            }

            if (apiRunnable.getApiMapping().useLogin()) {
                if (!apiRequest.isLogin()) {
                    throw new ApiException("009","调用失败：用户未登陆");
                }
            }

            logger.info("请求接口={" + method + "} 参数=" + params + "");
            Object[] args = buildParams(apiRunnable, params, request, response, apiRequest);
            result = apiRunnable.run(args);
        }catch (ApiException e) {
            response.setStatus(500);// 封装异常并返回
            logger.error("调用接口={" + method + "}异常  参数=" + params + "", e);
            result = handleError(e);
        } catch (InvocationTargetException e) {
            response.setStatus(500);// 封装业务异常并返回
            logger.error("调用接口={" + method + "}异常  参数=" + params + "", e.getTargetException());
            result = handleError(e.getTargetException());
        } catch (Exception e) {
            response.setStatus(500);// 封装业务异常并返回
            logger.error("其他异常", e);
            result = handleError(e);
        }
        //2.校验业务参数
        returnResult(result, response);
    }

    /**
     * 签名验证
     *  签名规则:md5(secret+method+param+token+timestamp+secret)
     * @param apiRequest
     * @return
     * @throws ApiException
     */
    private ApiRequest signCheck(ApiRequest apiRequest) throws ApiException {
        Token token = tokenService.getToken(apiRequest.getAccessToken());
        //1. 验证token是否存在
        if (token == null) {
            throw new ApiException("验证失败：指定'Token'不存在");
        }
        //2.验证token是否过期
        if (token.getExpiresTime().before(new Date())) {
            throw new ApiException("验证失败：'Token'过期");
        }

        if (Math.abs(Long.parseLong(apiRequest.getTimestamp()) - System.currentTimeMillis()) > 1000*60*1000) {
            throw new ApiException("验证失败：'Token'过期");
        }

        // 3.签名规则:md5(secret+method+param+token+timestamp+secret)
        String secret = token.getSecret();
        String method = apiRequest.getMethodName();
        String params = apiRequest.getParams();
        String accessToken = token.getAccessToken();
        String timestamp = apiRequest.getTimestamp();
        String sign = Md5Util.MD5(secret + method + params + accessToken + timestamp + secret);
        if (!sign.toUpperCase().equals(apiRequest.getSign())) {
            throw new ApiException("验证失败：签名非法");
        }

        apiRequest.setLogin(true);
        apiRequest.setMemberId(token.getMemberId());

        return apiRequest;
    }

    private ApiRequest buildApiRequest(HttpServletRequest request) {
        ApiRequest apiRequest = new ApiRequest();
        apiRequest.setAccessToken(request.getParameter("token"));
        apiRequest.setMethodName(request.getParameter(METHOD));
        apiRequest.setParams(request.getParameter(PARAMS));
        apiRequest.seteCode(request.getParameter("eCode"));
        apiRequest.setuCode(request.getParameter("uCode"));
        apiRequest.setTimestamp(request.getParameter("timestamp"));
        apiRequest.setSign(request.getParameter("sign"));
        apiRequest.setClientIp(request.getRemoteAddr());
        return apiRequest;
    }

    /**
     * 封装错误信息
     * @param throwable
     * @return
     */
    private Object handleError(Throwable throwable) {
        String code = "";
        String msg = "";

        if (throwable instanceof ApiException) {
            code = "0001";
            msg = throwable.getMessage();
        } else {
            code = "0002";
            msg = throwable.getMessage();
        }
        Map<String, String> result = new HashMap<>();
        result.put("error",code);
        result.put("msg",msg);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        throwable.printStackTrace(pw);

        return result;
    }

    /**
     * 将请求结果相应给客户端
     * @param result
     * @param response
     */
    private void returnResult(Object result, HttpServletResponse response) {
        try {
            UtilJson.JSON_MAPPER.configure(SerializationFeature.WRITE_NULL_MAP_VALUES,true);
            String json = UtilJson.writeValueAsString(result);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html/json;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            if (json != null) {
                response.getWriter().write(json);
            }
        } catch (IOException e) {
            logger.error("服务中心响应异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建参数
     * @param apiRunnable
     * @param params
     * @param request
     * @param response
     * @param apiRequest
     * @return
     * @throws ApiException
     */
    private Object[] buildParams(ApiStore.ApiRunnable apiRunnable, String params, HttpServletRequest request, HttpServletResponse response, ApiRequest apiRequest) throws ApiException {
        Map<String, Object> map = null;
        try {
            map = UtilJson.toMap(params);
        } catch (IllegalArgumentException e) {
            throw new ApiException("调用失败：json字符串格式异常，请检查params参数 ");
        }

        if (map == null) {
            map = new HashMap<>();
        }

        Method method = apiRunnable.getTargetMethod();
        List<String> paramNames = Arrays.asList(parameterUtil.getParameterNames(method));
        Class[] paramTypes = apiRunnable.getParamTypes();

        for (Map.Entry<String, Object> m : map.entrySet()) {
            if (!paramNames.contains(m.getKey())) {
                throw new ApiException("调用失败：接口不存在‘" + m.getKey() + "’参数");
            }
        }

        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].isAssignableFrom(HttpServletRequest.class)) {
                args[i] = request;
            }else if (paramTypes[i].isAssignableFrom(ApiRequest.class)){
                args[i] = apiRequest;
            }else if (map.containsKey(paramNames.get(i))){
                try {
                    args[i] = convertJsonToBean(map.get(paramNames.get(i)), paramTypes[i]);
                } catch (Exception e) {
                    throw new ApiException("调用失败：指定参数格式错误或值错误‘" + paramNames.get(i) + "’"
                            + e.getMessage());
                }
            }else {
                args[i] = null;
            }
        }
        return args;
    }

    /**
     * 将参数转换成相应的数据类型
     * @param val
     * @param targetClass
     * @return
     */
    private Object convertJsonToBean(Object val, Class targetClass) {
        Object result = null;
        if (val == null) {
            return null;
        }else if (Integer.class.equals(targetClass)){
            result = Integer.parseInt(val.toString());
        }else if (Long.class.equals(targetClass)) {
            result = Long.parseLong(val.toString());
        }else if (Date.class.equals(targetClass)) {
            if (val.toString().matches("[0-9]+")) {
                result = new Date(Long.parseLong(val.toString()));
            }else {
                throw new IllegalArgumentException("日期必须是长整型的时间戳");
            }
        }else if (String.class.equals(targetClass)) {
            if (val instanceof String) {
                result = val;
            }else {
                throw new IllegalArgumentException("转换目标类型为字符串");
            }
        }else {
            result = UtilJson.convertValue(val, targetClass);
        }

        return result;
    }

    /**
     * 校验系统参数
     * @param request
     * @return
     * @throws ApiException
     */
    private ApiStore.ApiRunnable sysParamsValdate(HttpServletRequest request) throws ApiException {
        String apiName = request.getParameter(METHOD);
        String params = request.getParameter(PARAMS);
        ApiStore.ApiRunnable apiRunnable;
        if (apiName == null || apiName.trim().equals("")) {
            throw new ApiException("调用失败：参数'method'为空");
        } else if (params == null) {
            throw new ApiException("调用失败：参数'params'为空");
        } else if ((apiRunnable = apiStore.findApiRunnable(apiName)) == null) {
            throw new ApiException("调用失败：指定API不存在，API:" + apiName);
        }
        return apiRunnable;
    }
}
