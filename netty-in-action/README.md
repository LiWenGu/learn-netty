重点学习 dubbo-remoting-netty4

# 1. Netty 在 Dubbo 的实战学习

## 1.1 Netty 的 IO 模型在 Dubbo 中的体现

Netty IO 模型在，Dubbo Dispatcher 机制中的体现：

Dubbo 中为了让 Netty 作为远程插件，用了装饰器模式：org.apache.dubbo.remoting.transport.netty4.NettyServerHandler，该方法为
Netty 远程的 Handler 处理器，装饰了 Dubbo 通用的的 Handler，作为构造参数传入：
```java
public NettyServerHandler(URL url, ChannelHandler handler) {
    if (url == null) {
        throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
        throw new IllegalArgumentException("handler == null");
    }
    this.url = url;
    this.handler = handler;
}
```

第二步则是优化是对 IO/业务线程的隔离，为了通用性，使用了业务自定义线程池，具体做法是对 hanlder 做封装，
即 dubbo 的 dispatcher 特性，通过对 handler 封装，定义自定义线程池，来对连接的读写以及断连操作选择性的做异步化，具体有
如下 dispatcher 组合方式：  
all：connected、disconnected、received、caught 使用自定义线程池  
connection：connected、disconnected 使用单线程处理，received、caught 使用自定义线程池  
direct：received 使用自定义线程池  
execution：received 使用自定义线程池  
message：received 使用自定义线程池  
  
注意：在 2.7.5 中优化了客户端连接池，即 ThreadlessExecutor，该线程池是没有管理的。

我们知道 Netty 支持两种方式自定义线程池来处理业务逻辑，一是使用 `io.netty.channel.ChannelPipeline.addLast(io.netty.util.concurrent.EventExecutorGroup, java.lang.String, io.netty.channel.ChannelHandler)`  
二是使用在 Handler 中使用自定义线程池。前者和 workNioEventLoop 绑定，减少了上下文切换，而后者所有的 workNioEventLoop 都在竞争，性能肯定不如前者的。  
  
这个问题：https://github.com/apache/dubbo/issues/6605

![20200816163606](https://markdownnoteimages.oss-cn-hangzhou.aliyuncs.com/20200816163606.png)  
  
![20200816163629](https://markdownnoteimages.oss-cn-hangzhou.aliyuncs.com/20200816163629.png)

## 1.2 Netty 的心跳/重连机制在 Dubbo 中的体现

Dubbo Exchange 层的客户端有重连/心跳的任务：`org.apache.dubbo.remoting.exchange.support.header.HeaderExchangeClient.HeaderExchangeClient`  
```java
public HeaderExchangeClient(Client client, boolean startTimer) {
    Assert.notNull(client, "Client can't be null");
    this.client = client;
    this.channel = new HeaderExchangeChannel(client);

    if (startTimer) {
        URL url = client.getUrl();
        // 重连检查任务
        startReconnectTask(url);
        // 心跳检查任务
        startHeartBeatTask(url);
    }
}
```

Dubbo Exchange 层的服务端只有超时关闭的任务：`org.apache.dubbo.remoting.exchange.support.header.HeaderExchangeServer.HeaderExchangeServer`
```java
public HeaderExchangeServer(RemotingServer server) {
    Assert.notNull(server, "server == null");
    this.server = server;
    // 检查空闲连接关闭
    startIdleCheckTask(getUrl());
}
```

Dubbo Transport 层的 NettyClient 的连接空闲检查：  
```java
ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
.addLast("decoder", adapter.getDecoder())
.addLast("encoder", adapter.getEncoder())
.addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS))
.addLast("handler", nettyClientHandler);
```  

Dubbo Transport 层的 NettyServer 的连接空闲检查：  
```java
ch.pipeline()
.addLast("decoder", adapter.getDecoder())
.addLast("encoder", adapter.getEncoder())
.addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
.addLast("handler", nettyServerHandler);
```  

从 NettyClientHandler 和 NettyServerHandler 中可以得知，当 NettyClient 发现有连接空闲时，主动发心跳类型的消息。而 NettyServer 发现有连接空闲时，直接断开连接。  
Client 端的 Exchange 层的重连/心跳任务和 Transport（NettyClient）的连接空闲任务是否重复了？  
Server 端的 Exchange 层的超时关闭任务和 Transport（NettyServer）的连接空闲关闭连接是否又重复了呢？

## 1.3 Netty 的 option 在 Dubbo 的体现

NettyServer：  
```java
bootstrap.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
    .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
```

NettyClient：  
```java
bootstrap.group(nioEventLoopGroup)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
        .channel(NioSocketChannel.class);

bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(3000, getConnectTimeout()));
```

## 1.4 Netty 的编解码在 Dubbo 的体现（Dubbo Serialize）