package com.demo.netty.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import static com.demo.netty.gateway.runner.NettyGatewayRunner.GATEWAY_NAME;
import static com.demo.netty.gateway.runner.NettyGatewayRunner.GATEWAY_VERSION;

public class HeaderHttpRequestFilter implements HttpRequestFilter{

    @Override
    public void filter(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {
        fullRequest.headers().set("gateway-name", GATEWAY_NAME);
        fullRequest.headers().set("gateway-version", GATEWAY_VERSION);
    }
}
