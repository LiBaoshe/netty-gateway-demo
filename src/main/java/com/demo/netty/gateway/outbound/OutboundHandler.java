package com.demo.netty.gateway.outbound;

import com.demo.netty.gateway.filter.HttpRequestFilter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.List;
import java.util.stream.Collectors;

public abstract class OutboundHandler {

    public abstract void handle(FullHttpRequest fullRequest, ChannelHandlerContext ctx, HttpRequestFilter filter);

    protected List<String> getBackendUrls(List<String> backends){
        return backends.stream().map(this::formatUrl).collect(Collectors.toList());
    }

    public String formatUrl(String backend){
        return backend.endsWith("/") ? backend.substring(0, backend.length() - 1) : backend;
    }

    protected void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    protected void handleResponse(){

    }
}
