package com.demo.netty.gateway.outbound.okhttp;

import com.demo.netty.gateway.filter.HttpRequestFilter;
import com.demo.netty.gateway.outbound.OutboundHandler;
import com.demo.netty.gateway.outbound.factory.NamedThreadFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * 通过 OkHttp 访问后端服务
 */
public class OkhttpOutboundHandler extends OutboundHandler {

    final OkHttpClient okHttpClient;
    final private ExecutorService proxyService;
    final private List<String> backendUrls;

    public OkhttpOutboundHandler(List<String> backends) {

        this.backendUrls = super.getBackendUrls(backends);

        int cores = Runtime.getRuntime().availableProcessors();
        long keepAliveTime = 1000;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new  ThreadPoolExecutor.CallerRunsPolicy();
        proxyService = new ThreadPoolExecutor(
                cores,
                cores,
                keepAliveTime,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxyService"),
                handler
        );

        okHttpClient = new OkHttpClient.Builder()
                .readTimeout(5,TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void handle(FullHttpRequest fullRequest, ChannelHandlerContext ctx, HttpRequestFilter filter) {
        String backendUrl = router.route(this.backendUrls);
        final String url = backendUrl + fullRequest.uri();
        filter.filter(fullRequest, ctx);
        proxyService.submit(() -> fetchGet(fullRequest, ctx, url));
    }

    private void fetchGet(final FullHttpRequest inbound, final ChannelHandlerContext ctx, final String url) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("connection", "keep-alive")
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(inbound, ctx, response);
            }

        });
    }

    private void handleResponse(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, Response endpointResponse) {
        FullHttpResponse response = null;
        try {
            byte[] body = endpointResponse.body().bytes();
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(body));
            response.headers().set("Content-Type", "application/json");
            response.headers().set("Content-Length", endpointResponse.body().contentLength());
            System.out.println(endpointResponse.body().contentLength());
            filter.filter(response);
        } catch (IOException e) {
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            if(fullRequest != null){
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            }
            ctx.flush();
        }
    }
}
