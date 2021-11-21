package com.demo.netty.gateway.outbound.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;

/**
 * Netty 发送 http 请求
 */
public class NettyHttpClient {

    public void connect(String url, final NettyHttpResp nettyHttpResp) throws Exception {

        URI uri = URI.create(url);
        String host = uri.getHost();
        int port = uri.getPort();

        System.out.println("使用 NettyHttpClient 访问：" + url);

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_RCVBUF, 32 * 1024)
                    .option(ChannelOption.SO_SNDBUF, 32 * 1204)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            // 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
                            ch.pipeline().addLast(new HttpResponseDecoder());
                            // 客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
                            ch.pipeline().addLast(new HttpRequestEncoder());
                            ch.pipeline().addLast(new NettyHttpClientHandler(nettyHttpResp));
                        }
                    });
            ChannelFuture f = bootstrap.connect(host, port).sync();
            workerGroup.submit(() -> {
                String msg = "Are you ok?";
                DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                        HttpVersion.HTTP_1_1, HttpMethod.POST, uri.toASCIIString(),
                        Unpooled.wrappedBuffer(msg.getBytes()));
                // 构建http请求
                request.headers().set(HttpHeaderNames.HOST, host);
                request.headers().set(HttpHeaderNames.CONNECTION,
                        HttpHeaderNames.CONNECTION);
                request.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                        request.content().readableBytes());
                request.headers().set("messageType", "normal");
                request.headers().set("businessType", "testServerState");
                // 发送http请求
                f.channel().write(request);
                f.channel().flush();
            });
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    private static ByteBufToBytes reader;

    public static void main(String[] args) throws Exception {
        // 测试 NettyHttpClient
        NettyHttpClient client = new NettyHttpClient();
        String[] urls = new String[]{"http://127.0.0.1:8801", "http://127.0.0.1:8802", "http://127.0.0.1:8803"};
        for (int i = 0; i < 10; i++) {
            test(client, urls[i % 3]);
        }
    }

    private static void test(NettyHttpClient client, String url) throws Exception {
        client.connect(url, ((ctx, msg) -> {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;
                System.out.println("CONTENT_TYPE:" + response.headers().get(HttpHeaderNames.CONTENT_TYPE));
                if (HttpUtil.isContentLengthSet(response)) {
                    reader = new ByteBufToBytes(
                            (int) HttpUtil.getContentLength(response));
                }
            }
            if (msg instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) msg;
                ByteBuf content = httpContent.content();
                reader.reading(content);
                content.release();
                if (reader.isEnd()) {
                    String resultStr = new String(reader.readFull());
                    System.out.println("Server said:" + resultStr);
                }
            }
            ctx.close();
        }));
    }
}
