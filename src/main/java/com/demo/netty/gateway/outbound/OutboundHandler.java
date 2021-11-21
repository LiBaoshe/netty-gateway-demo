package com.demo.netty.gateway.outbound;

import com.demo.netty.gateway.filter.HeaderResponseFilter;
import com.demo.netty.gateway.filter.HttpRequestFilter;
import com.demo.netty.gateway.filter.HttpResponseFilter;
import com.demo.netty.gateway.router.HttpEndpointRouter;
import com.demo.netty.gateway.router.RoundRibbonHttpEndpointRouter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 访问后端服务抽象类
 */
public abstract class OutboundHandler {

    // 定义默认 Response 过滤器
    protected HttpResponseFilter filter = new HeaderResponseFilter();
    // 定义默认路由规则
//    protected HttpEndpointRouter router = new RandomHttpEndpointRouter();
    protected HttpEndpointRouter router = new RoundRibbonHttpEndpointRouter();

    /**
     * 访问后端服务，由子类实现
     * @param fullRequest
     * @param ctx
     * @param filter
     */
    public abstract void handle(FullHttpRequest fullRequest, ChannelHandlerContext ctx, HttpRequestFilter filter);

    /**
     * 返回处理后的 url 地址集合
     * @param backends
     * @return
     */
    protected List<String> getBackendUrls(List<String> backends){
        return backends.stream().map(this::formatUrl).collect(Collectors.toList());
    }

    /**
     * 处理 url 地址格式
     * @param backend
     * @return
     */
    public String formatUrl(String backend){
        return backend.endsWith("/") ? backend.substring(0, backend.length() - 1) : backend;
    }

    /**
     * 发生异常时关闭通道
     * @param ctx
     * @param cause
     */
    protected void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
