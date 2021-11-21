package com.demo.netty.gateway.outbound.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyHttpClientHandler extends ChannelInboundHandlerAdapter {

    private final NettyHttpResp nettyHttpResp;

    public NettyHttpClientHandler(NettyHttpResp nettyHttpResp) {
        this.nettyHttpResp = nettyHttpResp;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        // 结果交给使用者处理
        nettyHttpResp.handle(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
