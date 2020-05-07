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
package io.netty.cases.chapter7;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 李林峰 on 2018/8/11.
 * Updated by liwenguang on 2020/05/08.
 * 线程安全，服务端一共打印 100008。但是是由用户保证线程安全性
 */
public class ThreadSecurityClient2 {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "18087"));
    static final int MSG_SIZE = 256;

    public static void main(String[] args) throws Exception {
        new ThreadSecurityClient2().run();
    }

    public void run() throws Exception {
        connect();
    }

    public void connect() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(8);
        Bootstrap b = new Bootstrap();
        ChannelHandler clientHanlder = new ThreadSecurityClient2().new SharableClientHandler();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler());
                        ch.pipeline().addLast(clientHanlder);
                    }
                });
        ChannelFuture f = null;
        for (int i = 0; i < 8; i++) {
            f = b.connect(HOST, PORT).sync();
        }
        f.channel().closeFuture().sync();
        group.shutdownGracefully();
    }

    @ChannelHandler.Sharable
    public class SharableClientHandler extends ChannelInboundHandlerAdapter {

        AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ByteBuf firstMessage = Unpooled.buffer(ThreadSecurityClient.MSG_SIZE);
            for (int i = 0; i < firstMessage.capacity(); i++) {
                firstMessage.writeByte((byte) i);
            }
            ctx.writeAndFlush(firstMessage);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf req = (ByteBuf) msg;
            if (counter.getAndIncrement() < 10000)
                ctx.write(msg);
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
