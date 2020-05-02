package io.netty.cases.chapter4;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultPromise;

import java.util.concurrent.ExecutionException;

/**
 * Created by 李林峰 on 2018/8/11.
 * Updated by liwenguang on 2020/05/02.
 * java.lang.UnsupportedOperationException: direct buffer
 */
public class HttpClient {

    private Channel channel;
    HttpClientHandler handler = new HttpClientHandler();

    private void connect(String host, int port) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new HttpClientCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(Short.MAX_VALUE));
                ch.pipeline().addLast(handler);
            }
        });
        ChannelFuture f = b.connect(host, port).sync();
        channel = f.channel();
    }

    private HttpResponse blockSend(FullHttpRequest request) throws InterruptedException, ExecutionException {
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        DefaultPromise<HttpResponse> respPromise = new DefaultPromise<>(channel.eventLoop());
        handler.setRespPromise(respPromise);
        channel.writeAndFlush(request);
        HttpResponse response = respPromise.get();
        if (response != null)
            System.out.print("The client received http response, the body is :" + new String(response.body()));
        return response;
    }

    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();
        client.connect("127.0.0.1", 18084);
        ByteBuf body = Unpooled.wrappedBuffer("Http message!".getBytes("UTF-8"));
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "http://127.0.0.1/user?id=10&addr=NanJing", body);
        HttpResponse response = client.blockSend(request);
    }

    class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        DefaultPromise<HttpResponse> respPromise;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx,
                                    FullHttpResponse msg) throws Exception {
            if (msg.decoderResult().isFailure())
                throw new Exception("Decode HttpResponse error : " + msg.decoderResult().cause());
            HttpResponse response = new HttpResponse(msg);
            respPromise.setSuccess(response);
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        public DefaultPromise<HttpResponse> getRespPromise() {
            return respPromise;
        }

        public void setRespPromise(DefaultPromise<HttpResponse> respPromise) {
            this.respPromise = respPromise;
        }

    }

    public class HttpResponse {

        private HttpHeaders header;

        private FullHttpResponse response;

        private byte[] body;

        public HttpResponse(FullHttpResponse response) {
            this.header = response.headers();
            this.response = response;
        }

        public HttpHeaders header() {
            return header;
        }

        /**
         * 实际抛出异常的地方：{@link io.netty.buffer.PooledUnsafeDirectByteBuf#array()}
         */
        public byte[] body() {
            return body = response.content() != null ?
                    response.content().array() : null;
        }
    }
}
