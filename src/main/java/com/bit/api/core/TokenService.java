package com.bit.api.core;

public interface TokenService {

    /**
     * 生成token
     * @return
     */
    Token createToken();

    /**
     * 获取token
     * @param token
     * @return
     */
    Token getToken(String token);
}
