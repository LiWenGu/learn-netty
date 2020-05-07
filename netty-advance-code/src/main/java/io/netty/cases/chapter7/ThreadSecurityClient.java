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

/**
 * Created by 李林峰 on 2018/8/11.
 * Updated by liwenguang on 2020/05/08.
 * 线程安全，原因在于启动多个客户端并发执行，但是每个 channel 都是用 new 创建各自的
 */
public class ThreadSecurityClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "18087"));
    static final int MSG_SIZE = 256;

    public static void main(String[] args) throws Exception {
        new ThreadSecurityClient().run();
    }

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
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler());
                        //每个链路都有自己对应的业务 Handler 实例，不共享
                        ch.pipeline().addLast(new NoThreadSecurityClientHandler());
                    }
                });
        ChannelFuture f = null;
        for (int i = 0; i < 8; i++) {
            f = b.connect(HOST, PORT).sync();
        }
        f.channel().closeFuture().sync();
        group.shutdownGracefully();
    }

    public class NoThreadSecurityClientHandler extends ChannelInboundHandlerAdapter {

        int counter = 0;

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
            if (counter++ < 10000)
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
