# 1. Netty 在 Dubbo 的实战学习

重点学习 dubbo-remoting-netty4

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
  
NettyServer：  
```java
bootstrap.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
    .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
```

### 1.3.1 option 和 childoption 

option 作用于 bossEventLoopGroup，childOption 作用于 workEventLoopGroup。即 option 用于连接的初始化、断开等，具体为bind 和 connect 方法会应用 option 选项。  
childoption 用于 workEventLoopGroup，即用于服务器连接后的服务器通道。 

### 1.3.2 SO_REUSEADDR 参数
 
SO_REUSEADDR提供如下四个功能：  
1. 允许启动一个监听服务器并捆绑其众所周知端口，即使以前建立的将此端口用做他们的本地端口的连接仍存在。这通常是重启监听服务器时出现，若不设置此选项，则bind时将出错  
2. 允许在同一端口上启动同一服务器的多个实例，只要每个实例捆绑一个不同的本地IP地址即可。对于TCP，我们根本不可能启动捆绑相同IP地址和相同端口号的多个服务器。  
3. 允许单个进程捆绑同一端口到多个套接口上，只要每个捆绑指定不同的本地IP地址即可。这一般不用于TCP服务器。  
4. SO_REUSEADDR允许完全重复的捆绑：  
当一个IP地址和端口绑定到某个套接口上时，还允许此IP地址和端口捆绑到另一个套接口上。一般来说，这个特性仅在支持多播的系统上才有，而且只对UDP套接口而言（TCP不支持多播）。  

### 1.3.3 TCP_NODELAY

TCP_NODELAY 设置为 true：禁用 Nagle 算法。  
Nagle’s Algorithm 是为了提高带宽利用率设计的算法，其做法是合并小的 TCP 包为一个，避免了过多的小报文的 TCP 头所浪费的带宽。如果开启了这个算法 （默认），则协议栈会累积数据直到以下两个条件之一满足的时候才真正发送出去：  
1. 积累的数据量到达最大的 TCP Segment Size  
2. 收到了一个 Ack

伪代码：  
```c
if there is new data to send
   if the window size >= MSS and available data is >= MSS
    send complete MSS segment now
  else
    if there is unconfirmed data still in the pipe
      enqueue data in the buffer until an acknowledge is received
    else
      send data immediately
    end if
  end if
end if
```  

如果没有设置 TCP_NODELAY 为 true 时（即默认情况）：  
A 服务请求 B，第一次 write 到达了 B（8行），但是如果 B 服务端只有再接收更多数据才会响应，那么 A 服务会选择等待（6行），那么就会阻塞直到 40ms（默认最大等待时间）。  

### 1.3.4 ALLOCATOR

设置 bytebuf 的分配器，在 netty 4.1 中默认为 PooledByteBufAllocator。

### 1.3.5 SO_KEEPALIVE

只看到 NettyClient 设置了该属性，但是 NettyServer 没有设置该属性

## 1.4 Netty 的编解码在 Dubbo 的体现（Dubbo Serialize）

使用装饰器模式：`org.apache.dubbo.remoting.transport.netty4.NettyCodecAdapter`，使用 `org.apache.dubbo.remoting.exchange.codec.ExchangeCodec`，最后在 `org.apache.dubbo.remoting.exchange.codec.ExchangeCodec.encodeRequest` 通过 SPI 方式
获取序列化方式，和 netty 关系不大，主要是用于装饰自定义的编解码器来适配 Netty。

# 2. Netty 在 RocketMQ 的实战学习

重点在于 rocketmq-remoting 模块  
rocketmq 涉及通信的过程：  
1. Broker 定时向 NameServer 上报 Topic 路由信息  
2. Producer 发送消息时，根据 Msg 的 Topic 先从本地缓存的 TopicPublishInfoTable 获取路由信息，如果获取不到则请求 NameServer 上重新拉取  
3. Producer 根据路由信息选择一个 MessageQueue 将消息发送给 Broker。

## 2.1 Netty 的 IO 模型在 RocketMQ 中的体现

### 2.1.1 客户端

不考虑 TLS。  
客户端代码：`org.apache.rocketmq.remoting.netty.NettyRemotingClient.start`  
使用了 `EventExecutorGroup` 策略。我们知道 Netty 使用自定义线程池有两种方式，一种是像 Dubbo 那样，在 handler 中自定义线程池，还有一种是在 pipeline 增加 EventExecutorGroup 参数，
后者好处在于每个 NioEventLoop 都和 EventExecutor 绑定，但是在 NettyRemotingClient 的 NioEventLoopGroup 是 1，而 defaultEventExecutorGroup 为 4，也就是 1 个 NIO 线程对应了 4 个
线程，仍然会有上下文切换的问题。  
具体可参考 #1.1 的线程绑定图：当 NioEventLoop 只有一个时：  
![20200816163606](https://markdownnoteimages.oss-cn-hangzhou.aliyuncs.com/20200816163606.png)  
  
总结：默认有 1 个 Nio 线程做链接请求，4 个线程用作 IO 业务操作。

### 2.1.2 服务端

代码：`org.apache.rocketmq.remoting.netty.NettyRemotingServer.start`  

总结：默认有 1 个 Nio 线程做链接请求，3 个线程用作 IO 读写操作，8 个线程做 IO 业务操作。  
我觉得这个没有 Dubbo 的灵活，Dubbo 通过 dispatcher，可以自由的配置各个操作是否在 io 线程还是业务线程操作。

## 2.2 Netty 的心跳/重连机制在 RocketMQ 中的体现

### 2.2.1 client(consumer/producer) -> broker 的心跳
  
关键位置：`org.apache.rocketmq.client.impl.factory.MQClientInstance.startScheduledTask` 里面有个每隔 1 秒就执行的定时任务，定时执行 `MQClientInstance.this.sendHeartbeatToAllBrokerWithLock()` 最终执行 `int version = this.mQClientAPIImpl.sendHearbeat(addr, heartbeatData, 3000);` 里面会调用同步请求
到 broker，并返回 version，接着客户端更新该 broker 的 version。当 broker 越多，心跳会越来越延迟最后不准确。  
```java
public int sendHearbeat(
        final String addr,
        final HeartbeatData heartbeatData,
        final long timeoutMillis
    ) throws RemotingException, MQBrokerException, InterruptedException {
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);
    request.setLanguage(clientConfig.getLanguage());
    request.setBody(heartbeatData.encode());
    RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
    assert response != null;
    switch (response.getCode()) {
        case ResponseCode.SUCCESS: {
            return response.getVersion();
        }
        default:
            break;
    }

    throw new MQBrokerException(response.getCode(), response.getRemark());
}
```

### 2.2.2 broker -> client(consumer/producer) 的心跳

broker 处理 client 的心跳关键代码：`org.apache.rocketmq.broker.processor.ClientManageProcessor.heartBeat`  
  
```java
public RemotingCommand heartBeat(ChannelHandlerContext ctx, RemotingCommand request) {
    RemotingCommand response = RemotingCommand.createResponseCommand(null);
    HeartbeatData heartbeatData = HeartbeatData.decode(request.getBody(), HeartbeatData.class);
    ClientChannelInfo clientChannelInfo = new ClientChannelInfo(
        ctx.channel(),
        heartbeatData.getClientID(),
        request.getLanguage(),
        request.getVersion()
    );

    for (ConsumerData data : heartbeatData.getConsumerDataSet()) {
        SubscriptionGroupConfig subscriptionGroupConfig =
            this.brokerController.getSubscriptionGroupManager().findSubscriptionGroupConfig(
                data.getGroupName());
        boolean isNotifyConsumerIdsChangedEnable = true;
        if (null != subscriptionGroupConfig) {
            isNotifyConsumerIdsChangedEnable = subscriptionGroupConfig.isNotifyConsumerIdsChangedEnable();
            int topicSysFlag = 0;
            if (data.isUnitMode()) {
                topicSysFlag = TopicSysFlag.buildSysFlag(false, true);
            }
            // 创建用于 consumer 重试的 Topic，用于消息重新消费
            String newTopic = MixAll.getRetryTopic(data.getGroupName());
            this.brokerController.getTopicConfigManager().createTopicInSendMessageBackMethod(
                newTopic,
                subscriptionGroupConfig.getRetryQueueNums(),
                PermName.PERM_WRITE | PermName.PERM_READ, topicSysFlag);
        }
        // 注册 consumer
        boolean changed = this.brokerController.getConsumerManager().registerConsumer(
            data.getGroupName(),
            clientChannelInfo,
            data.getConsumeType(),
            data.getMessageModel(),
            data.getConsumeFromWhere(),
            data.getSubscriptionDataSet(),
            isNotifyConsumerIdsChangedEnable
        );

        if (changed) {
            log.info("registerConsumer info changed {} {}",
                data.toString(),
                RemotingHelper.parseChannelRemoteAddr(ctx.channel())
            );
        }
    }

    // 注册 producer
    for (ProducerData data : heartbeatData.getProducerDataSet()) {
        this.brokerController.getProducerManager().registerProducer(data.getGroupName(),
            clientChannelInfo);
    }
    response.setCode(ResponseCode.SUCCESS);
    response.setRemark(null);
    return response;
}
```

###  2.2.2 broker/client(consumer/producer) -> nameserver 的心跳

核心代码：`org.apache.rocketmq.common.namesrv.TopAddressing.fetchNSAddr()`，被 broker 和 client 都用到，而且使用的定时任务配置是一样的，定时拉取 nameserver 信息：  

是通过 http 请求，而不是自定义 tcp 协议的 rpc 调用：  
```java
public final String fetchNSAddr(boolean verbose, long timeoutMills) {
    String url = this.wsAddr;
    try {
        if (!UtilAll.isBlank(this.unitName)) {
            url = url + "-" + this.unitName + "?nofix=1";
        }
        HttpTinyClient.HttpResult result = HttpTinyClient.httpGet(url, null, null, "UTF-8", timeoutMills);
        if (200 == result.code) {
            String responseStr = result.content;
            if (responseStr != null) {
                return clearNewLine(responseStr);
            } else {
                log.error("fetch nameserver address is null");
            }
        } else {
            log.error("fetch nameserver address failed. statusCode=" + result.code);
        }
    } catch (IOException e) {
        if (verbose) {
            log.error("fetch name server address exception", e);
        }
    }

    if (verbose) {
        String errorMsg =
            "connect to " + url + " failed, maybe the domain name " + MixAll.getWSAddr() + " not bind in /etc/hosts";
        errorMsg += FAQUrl.suggestTodo(FAQUrl.NAME_SERVER_ADDR_NOT_EXIST_URL);

        log.warn(errorMsg);
    }
    return null;
}
```

## 2.3 Netty 的 option 在 RocketMQ 的体现

NettyClient：  
```java
Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_KEEPALIVE, false)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis())
        .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize())
        .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getClientSocketRcvBufSize())
        .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                if (nettyClientConfig.isUseTLS()) {
                    if (null != sslContext) {
                        pipeline.addFirst(defaultEventExecutorGroup, "sslHandler", sslContext.newHandler(ch.alloc()));
                        log.info("Prepend SSL handler");
                    } else {
                        log.warn("Connections are insecure as SSLContext is null!");
                    }
                }
                pipeline.addLast(
                    defaultEventExecutorGroup,
                    new NettyEncoder(),
                    new NettyDecoder(),
                    new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()),
                    new NettyConnectManageHandler(),
                    new NettyClientHandler());
            }
        });
```
  
NettyServer：  
```java
ServerBootstrap childHandler =
    this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
        .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, false)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize())
        .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize())
        .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                    .addLast(defaultEventExecutorGroup, HANDSHAKE_HANDLER_NAME, handshakeHandler)
                    .addLast(defaultEventExecutorGroup,
                        encoder,
                        new NettyDecoder(),
                        new IdleStateHandler(0, 0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()),
                        connectionManageHandler,
                        serverHandler
                    );
            }
        });
```

### 2.3.1 TCP_NODELAY

client：option 设置为 false。  
server：childoption 设置为 false。  
和 dubbo 一致。

### 2.3.2 SO_KEEPALIVE

在 RocketMQ 中，client 和 server 都为 false。  
但是在 Dubbo 中 client 是 true，而 server 没有指定。但是 TCP 的 keepalive 是 2h，建议不打开，而且在 Dubbo 中该特性也没用。

### 2.3.3 SO_REUSEADDR

参考 dubbo。

### 2.3.4 CONNECT_TIMEOUT_MILLIS

客户端设置了超时时间，默认 3s。对于这个超时时间，网上资料是推荐加上这个，防止一直连接等待超时，但是在 Dubbo 是默认在上层就有超时设置的，达到了类似的效果（异步超时）。  

### 2.3.5 SO_BACKLOG

RocketMq-Netty-Server  
对应的是tcp/ip协议listen函数中的backlog参数，函数listen(int socketfd,int backlog)用来初始化服务端可连接队列，服务端处理客户端连接请求是顺序处理的，所以同一时间只能处理一个客户端连接，多个客户端来的时候，服务端将不能处理的客户端连接请求放在队列中等待处理，backlog参数指定了队列的大小

### 2.3.6 SO_SNDBUF/SO_RCVBUF

SO_RCVBUF和SO_SNDBUF每个套接口都有一个发送缓冲区和一个接收缓冲区，使用这两个套接口选项可以改变缺省缓冲区大小  

### 2.3.7 总结

从配置上来说，个人觉得 RocketMQ 是比 Dubbo 更加专业，至于是否有效率的提高，这个需要压测测试。
