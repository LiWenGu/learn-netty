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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 李林峰 on 2018/8/19.
 * Updated by liwenguang on 2020/05/12.
 * 使用 Netty 的 DefaultEventExecutorGroup，每个 TCP 一个连接，适用于 TCP 连接多的场景
 */
public final class ConcurrentPerformanceServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "18088"));
    static final EventExecutorGroup executor = new DefaultEventExecutorGroup(100);

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
                            ch.config().setAllocator(UnpooledByteBufAllocator.DEFAULT);
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(executor, new ConcurrentPerformanceServerHandler());
                        }
                    }).childOption(ChannelOption.SO_RCVBUF, 8 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 8 * 1024);
            ChannelFuture f = b.bind(PORT).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

class ConcurrentPerformanceServerHandler extends ChannelInboundHandlerAdapter {
    static AtomicInteger counter = new AtomicInteger(0);
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            int qps = counter.getAndSet(0);
            System.out.println("The server QPS is : " + qps);
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ((ByteBuf) msg).release();
        counter.incrementAndGet();
        //业务逻辑处理，模拟业务访问DB、缓存等，时延从100-1000毫秒之间不等
        Random random = new Random();
        try {
            TimeUnit.MILLISECONDS.sleep(random.nextInt(1000));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
