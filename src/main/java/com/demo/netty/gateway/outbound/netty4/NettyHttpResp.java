package com.demo.netty.gateway.outbound.netty4;

import io.netty.channel.ChannelHandlerContext;

/**
 * NettyHttp 返回结果处理接口，NettyHttpClient 的 connect 方法中回调 http 的请求结果
 */
@FunctionalInterface
public interface NettyHttpResp {

    void handle(ChannelHandlerContext ctx, Object msg);

}
