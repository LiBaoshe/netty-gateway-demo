package com.demo.netty.gateway.router;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRibbonHttpEndpointRouter implements HttpEndpointRouter {

    private static volatile AtomicInteger count = new AtomicInteger(0);

    @Override
    public String route(List<String> urls) {
        int index = count.getAndIncrement() / 2;
        if(index >= urls.size()){
            index = 0;
            count.set(1);
        }
        System.out.println("index = " + index);
        return urls.get(index);
    }
}
