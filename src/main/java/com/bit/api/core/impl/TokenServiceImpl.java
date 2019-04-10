package com.bit.api.core.impl;

import com.bit.api.core.Token;
import com.bit.api.core.TokenService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenServiceImpl implements TokenService {
    private static final Map<String, Token> tokens = new HashMap<>();
    static {
        for (int i=0;i<10;i++) {
            Token token = new Token();
            token.setId(i);
            token.setMemberId(i+"");
            token.setAccessToken("5832cef0-cc35-4383-8932-c71db1aed4bb-"+i);
            token.setSecret("5b1c4c5f-e82e-4b5f-9448-de6e6b949113-"+i);
            token.setClientIp("127.0.0.1");
            token.setClientType("0");
            token.setCreatedTime(new Date(1554909654601L));
            token.seteCode("thinkPad");
            token.setuCode("admin");
            token.setExpiresTime(new Date(1554909654601L+1000*60*1000));
            tokens.put("5832cef0-cc35-4383-8932-c71db1aed4bb-"+i,token);
        }

    }
    @Override
    public Token createToken() {
        Token token = new Token();
        token.setId(0);
        token.setMemberId("0");
        token.setAccessToken("5832cef0-cc35-4383-8932-c71db1aed4bb-0");
        token.setSecret("5b1c4c5f-e82e-4b5f-9448-de6e6b949113-0");
        token.setClientIp("127.0.0.1");
        token.setClientType("0");
        token.setCreatedTime(new Date(1554909654601L));
        token.seteCode("thinkPad");
        token.setuCode("admin");
        token.setExpiresTime(new Date(1554909654601L+1000*60*1000));

        return token;
    }

    @Override
    public Token getToken(String token) {
        return tokens.get(token);
    }

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString());
        System.out.println(System.currentTimeMillis());
    }
}
