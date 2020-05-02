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
package io.netty.cases.chapter3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by 李林峰 on 2018/8/5.
 * Updated by liwenguang on 2020/05/02.
 * 继承自 SimpleChannelInboundHandler 后不会出现 OutOfDirectMemoryError 异常
 */
public final class RouterServerUnpooledV2 {

    static final int PORT = Integer.parseInt(System.getProperty("port", "18083"));

    public static void main(String[] args) throws Exception {
        // Configure the server.
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
                            ch.config().setAllocator(UnpooledByteBufAllocator.DEFAULT);
                            p.addLast(new RouterServerHandlerV2());
                        }
                    });

            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    static class RouterServerHandlerV2 extends SimpleChannelInboundHandler<ByteBuf> {
        static ExecutorService executorService = Executors.newSingleThreadExecutor();
        PooledByteBufAllocator allocator;

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            byte[] body = new byte[msg.readableBytes()];
            executorService.execute(() ->
            {
                if (allocator == null)
                    allocator = new PooledByteBufAllocator(false);
                //解析请求消息，做路由转发，代码省略...
                //转发成功，返回响应给客户端
                ByteBuf respMsg = allocator.heapBuffer(body.length);
                respMsg.writeBytes(body);//作为示例，简化处理，将请求返回
                ctx.writeAndFlush(respMsg);
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
