package com.bit.api.core;

public class ApiRequest {
    /**
     * 会员ID
     */
    private String memberId;
    /**
     * Token
     */
    private String accessToken;
    /**
     * 签名
     */
    private String sign;
    /**
     * 设备用户标识
     */
    private String uCode;
    /**
     * 设备标识
     */
    private String eCode;
    /**
     * 时间戳
     */
    private String timestamp;
    /**
     * 客户端IP
     */
    private String clientIp;
    /**
     * 是否登录
     */
    private boolean isLogin;
    /**
     * 请求参数
     */
    private String params;
    /**
     * API接口名称
     */
    private String methodName;

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getuCode() {
        return uCode;
    }

    public void setuCode(String uCode) {
        this.uCode = uCode;
    }

    public String geteCode() {
        return eCode;
    }

    public void seteCode(String eCode) {
        this.eCode = eCode;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
