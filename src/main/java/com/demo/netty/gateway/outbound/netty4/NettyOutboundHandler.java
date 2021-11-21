package com.demo.netty.gateway.outbound.netty4;

import com.demo.netty.gateway.filter.HttpRequestFilter;
import com.demo.netty.gateway.outbound.OutboundHandler;
import com.demo.netty.gateway.outbound.factory.NamedThreadFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.List;
import java.util.concurrent.*;

/**
 * 通过 Netty 访问后端服务
 */
public class NettyOutboundHandler extends OutboundHandler {

    final private ExecutorService proxyService;
    final private List<String> backendUrls;
    final private NettyHttpClient nettyHttpClient;

    public NettyOutboundHandler(List<String> backends) {
        this.backendUrls = backends;

        int cores = Runtime.getRuntime().availableProcessors();
        long keepAliveTime = 1000;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        proxyService = new ThreadPoolExecutor(
                cores,
                cores,
                keepAliveTime,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxyService"),
                handler);

        nettyHttpClient = new NettyHttpClient();
    }

    @Override
    public void handle(FullHttpRequest fullRequest, ChannelHandlerContext ctx, HttpRequestFilter filter) {
        String backendUrl = router.route(this.backendUrls);
        final String url = backendUrl + fullRequest.uri();
        filter.filter(fullRequest, ctx);
        proxyService.submit(() -> fetchGet(fullRequest, ctx, url));
    }

    private ByteBufToBytes reader;

    private void fetchGet(FullHttpRequest inbound, ChannelHandlerContext ctx, final String url) {
        try {
            // 发送 Netty 请求
            nettyHttpClient.connect(url, (nettyCtx, msg) -> {
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
                        handleResponse(inbound, ctx, reader.readFull(), reader.contentLength);
                    }
                }
                nettyCtx.close();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponse(FullHttpRequest fullRequest, ChannelHandlerContext ctx, byte[] body, int contentLength) {
        FullHttpResponse response = null;
        try {
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(body));
            response.headers().set("Content-Type", "application/json");
            response.headers().setInt("Content-Length", contentLength);
            filter.filter(response);
        } catch (Exception e){
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            if(fullRequest != null){
                if(!HttpUtil.isKeepAlive(fullRequest)){
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.write(response);
                }
            }
            ctx.flush();
        }
    }
}
