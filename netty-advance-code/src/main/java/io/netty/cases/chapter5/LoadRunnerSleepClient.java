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
package io.netty.cases.chapter5;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Created by 李林峰 on 2018/8/11.
 * Updated by liwenguang on 2020/05/02.
 * 发送端1ms发送一次消息
 * 服务端读取速度慢，使用 debug 模拟：{@link io.netty.cases.chapter5.EchoServer.EchoServerHandler#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)}
 */
public final class LoadRunnerSleepClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "18085"));

    @SuppressWarnings({"unchecked", "deprecation"})
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new LoadRunnerSleepClientHandler());
                        }
                    });
            ChannelFuture f = b.connect(HOST, PORT).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static class LoadRunnerSleepClientHandler extends ChannelInboundHandlerAdapter {

        private final ByteBuf firstMessage;

        Runnable loadRunner;

        AtomicLong sendSum = new AtomicLong(0);

        Runnable profileMonitor;

        static final int SIZE = Integer.parseInt(System.getProperty("size", "10240"));

        /**
         * Creates a client-side handler.
         */
        public LoadRunnerSleepClientHandler() {
            firstMessage = Unpooled.buffer(SIZE);
            for (int i = 0; i < firstMessage.capacity(); i ++) {
                firstMessage.writeByte((byte) i);
            }
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            loadRunner = new Runnable() {
                @Override
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ByteBuf msg = null;
                    while(true)
                    {
                        byte [] body = new byte[SIZE];
                        msg = Unpooled.wrappedBuffer(body);
                        ctx.writeAndFlush(msg);
                        try {
                            TimeUnit.MILLISECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            new Thread(loadRunner, "LoadRunner-Thread").start();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg)
        {
            ReferenceCountUtil.release(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}