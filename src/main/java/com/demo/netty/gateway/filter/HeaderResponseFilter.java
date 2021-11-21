package com.demo.netty.gateway.filter;

import io.netty.handler.codec.http.FullHttpResponse;

public class HeaderResponseFilter implements HttpResponseFilter{

    @Override
    public void filter(FullHttpResponse response) {
        response.headers().set("kk", "java-1-nio");
    }
}
