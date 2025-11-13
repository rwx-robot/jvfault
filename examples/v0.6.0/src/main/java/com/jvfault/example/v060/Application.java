package com.jvfault.example.v060;

import com.jvfault.microservices.JacksonMessageCodec;
import com.jvfault.microservices.Message;
import com.jvfault.microservices.TransportClient;
import com.jvfault.microservices.RequestReplyClientFactory;
import com.jvfault.microservices.RequestPattern;
import com.jvfault.transport.tcp.TcpTransportClient;
import com.jvfault.transport.tcp.TcpTransportServer;

import java.util.concurrent.ThreadLocalRandom;

public class Application {

    interface MathApi {
        @RequestPattern("math.sum")
        int sum(int[] values);

        void log(String event);
    }

    public static void main(String[] args) {
        System.out.println("== jvfault v0.6.0: Microservices ==");
        int port = 27000 + ThreadLocalRandom.current().nextInt(2000);

        TcpTransportServer server = new TcpTransportServer(port);
        server.subscribe("math.sum", req -> {
            JacksonMessageCodec codec = new JacksonMessageCodec();
            int[] values = codec.decode(req.getData(), int[].class);
            int total = 0;
            for (int v : values) total += v;
            return new Message(req.getPattern(), codec.encode(total), req.getId(), null);
        });
        server.bind();
        System.out.println("  TCP server 启动于 127.0.0.1:" + port);

        TransportClient client = new TcpTransportClient("127.0.0.1", port);
        MathApi api = new RequestReplyClientFactory(client, 5000).create(MathApi.class);
        System.out.println("  math.sum([19, 23]) = " + api.sum(new int[]{19, 23}));
        api.log("event-over-tcp");
        System.out.println("  事件发射 + 类型安全客户端代理 全部通过");

        client.close();
        server.close();
        System.out.println("== 完成 ==");
    }
}
