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
 * 设置高水位
 */
public final class LoadRunnerWaterClient {

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
                    // 设置高水位
                    .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 10 * 1024 * 1024)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new LoadRunnerWaterClientHandler());
                        }
                    });
            ChannelFuture f = b.connect(HOST, PORT).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static class LoadRunnerWaterClientHandler extends ChannelInboundHandlerAdapter {

        private final ByteBuf firstMessage;

        Runnable loadRunner;

        AtomicLong sendSum = new AtomicLong(0);

        Runnable profileMonitor;

        static Logger LOG = Logger.getLogger(LoadRunnerWaterClientHandler.class.getName());

        static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

        /**
         * Creates a client-side handler.
         */
        public LoadRunnerWaterClientHandler() {
            firstMessage = Unpooled.buffer(SIZE);
            for (int i = 0; i < firstMessage.capacity(); i++) {
                firstMessage.writeByte((byte) i);
            }
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            ctx.channel().config().setWriteBufferHighWaterMark(10 * 1024 * 1024);
            loadRunner = new Runnable() {
                @Override
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ByteBuf msg = null;
                    while (true) {
                        // 判断是否可写（是否达到了高水位）
                        if (ctx.channel().isWritable()) {
                            msg = Unpooled.wrappedBuffer("Netty OOM Example".getBytes());
                            ctx.writeAndFlush(msg);
                        } else {
                            LOG.warning("The write queue is busy : " + ctx.channel().unsafe().outboundBuffer().nioBufferSize());
                        }
                    }
                }
            };
            new Thread(loadRunner, "LoadRunner-Thread").start();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ReferenceCountUtil.release(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}