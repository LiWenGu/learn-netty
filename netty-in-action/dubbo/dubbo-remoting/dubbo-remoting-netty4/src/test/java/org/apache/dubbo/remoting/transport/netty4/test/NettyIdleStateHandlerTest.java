package org.apache.dubbo.remoting.transport.netty4.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 测试空闲连接检查
 */
public class NettyIdleStateHandlerTest {

    @Test
    public void server() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();
        serverBootstrap.group(boss, work).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS))
                                .addLast(new StringDecoder())
                                .addLast(new HeartBeatServerHandler());
                    }
                });

        ChannelFuture f = serverBootstrap.bind(10909).sync();
        f.channel().closeFuture().sync();
    }

    @Test
    public void client() throws InterruptedException {

        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup clientWork = new NioEventLoopGroup();
        bootstrap.group(clientWork).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS))
                                .addLast(new StringEncoder())
                                .addLast(new HeartBeatClientHandler());
                    }
                });
        ChannelFuture future = bootstrap.connect("127.0.0.1", 10909).sync();
        if (future.isSuccess()) {

        }
        else if (future.cause() != null) {
            System.out.println(future.cause().getMessage() + future.cause());
        } else {
            System.out.println("失败");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        ChannelFuture channelFuture = future.channel().writeAndFlush("client pp");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        Thread.sleep(1000000);
    }

    static class HeartBeatServerHandler extends ChannelInboundHandlerAdapter {

        int lossConnectCount = 0;

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            System.out.println("已经 5 秒未收到客户端的消息了");
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.READER_IDLE) {
                    lossConnectCount++;
                    if (lossConnectCount > 2) {
                        System.out.println("关闭这个不活跃通道");
                        ctx.channel().close();
                    }
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            lossConnectCount = 0;
            System.out.println("Client says: " + msg.toString());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }


    }

    static class HeartBeatClientHandler extends ChannelInboundHandlerAdapter {

        int lossConnectCount = 0;

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            System.out.println("客户端循环心跳检测发送:" + new Date());
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.WRITER_IDLE) {
                    System.out.println("客户端触发写空闲");
                    ctx.writeAndFlush("biubiu");
                }
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println(msg);
            super.channelRead(ctx, msg);
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println(cause);
            super.exceptionCaught(ctx, cause);
        }
    }
}
