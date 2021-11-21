package com.demo.netty.gateway.runner;

import com.demo.netty.gateway.inbound.HttpInboundServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Spring boot 启动后开启 Netty 网关服务
 */
@Component
public class NettyGatewayRunner implements ApplicationRunner {

    public final static String GATEWAY_NAME = "Netty Gateway";
    public final static String GATEWAY_VERSION = "3.0.0";

    @Value("${server.port:8888}")
    private int serverPort;

    @Value("${netty.gateway.proxy.servers:''}")
    private String proxyServers;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println(GATEWAY_NAME + " " + GATEWAY_VERSION + " starting...");
        HttpInboundServer server = new HttpInboundServer(serverPort, Arrays.asList(proxyServers.split(",")));
        System.out.println(GATEWAY_NAME + " " + GATEWAY_VERSION + " started at http://localhost:" + serverPort + " for server: " + server.toString());
        server.run();
    }
}
