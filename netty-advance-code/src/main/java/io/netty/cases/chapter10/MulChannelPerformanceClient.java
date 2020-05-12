/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.cases.chapter10;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by 李林峰 on 2018/8/19.
 * Updated by liwenguang on 2020/05/12.
 * 100 个 TCP 连接，每个连接为 1qps，一共也是 100qps
 */
public class MulChannelPerformanceClient {

    final String HOST = System.getProperty("host", "127.0.0.1");
    final int PORT = Integer.parseInt(System.getProperty("port", "18088"));
    static final int MSG_SIZE = 256;

    public void run() throws Exception {
        connect();
    }

    public void connect() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(8);
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ConcurrentPerformanceClientHandlerV2());
                    }
                });
        ChannelFuture f = null;
        for (int i = 0; i < 100; i++) {
            f = b.connect(HOST, PORT).sync();
        }
        f.channel().closeFuture().sync();
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        new MulChannelPerformanceClient().run();
    }

    public class ConcurrentPerformanceClientHandlerV2 extends ChannelInboundHandlerAdapter {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                ByteBuf firstMessage = Unpooled.buffer(ConcurrentPerformanceClient.MSG_SIZE);
                for (int k = 0; k < firstMessage.capacity(); k++) {
                    firstMessage.writeByte((byte) k);
                }
                ctx.writeAndFlush(firstMessage);
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}


