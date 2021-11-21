package com.demo.netty.gateway.filter;

import io.netty.handler.codec.http.FullHttpResponse;

import static com.demo.netty.gateway.runner.NettyGatewayRunner.GATEWAY_NAME;
import static com.demo.netty.gateway.runner.NettyGatewayRunner.GATEWAY_VERSION;

public class HeaderResponseFilter implements HttpResponseFilter{

    @Override
    public void filter(FullHttpResponse response) {
        response.headers().set("gateway-name", GATEWAY_NAME);
        response.headers().set("gateway-version", GATEWAY_VERSION);
    }
}
