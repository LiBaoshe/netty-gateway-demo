package com.demo.netty.gateway.router;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RoundRibbon 轮询访问路由策略
 */
public class RoundRibbonHttpEndpointRouter implements HttpEndpointRouter {

    private static volatile AtomicInteger count = new AtomicInteger(0);

    @Override
    public String route(List<String> urls) {
        int index = count.getAndIncrement();
        if(index >= urls.size()){
            index = 0;
            count.set(0);
        }
        return urls.get(index);
    }

}
