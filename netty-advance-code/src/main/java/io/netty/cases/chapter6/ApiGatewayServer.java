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
package io.netty.cases.chapter6;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by 李林峰 on 2018/8/11.
 * Updated by liwenguang on 2020/05/07.
 * 每个请求，线程池都会引用 dispatchReq，如果堆积，线程池队列会引用多个 dispatchReq
 */
public final class ApiGatewayServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "18086"));

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new ApiGatewayServerHandler());
                        }
                    });
            ChannelFuture f = b.bind(PORT).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static class ApiGatewayServerHandler extends ChannelInboundHandlerAdapter {

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ctx.write(msg);
            char[] req = new char[64 * 1024];
            executorService.execute(() ->
            {
                char[] dispatchReq = req;
                //简单处理之后，转发请求消息到后端服务，后续代码省略
                try {
                    TimeUnit.MICROSECONDS.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

}
