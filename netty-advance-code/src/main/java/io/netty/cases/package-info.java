/**
 * 对应书顺序代码
 *
 * 第一章
 * @see io.netty.cases.chapter1.EchoExitServer1
 * @see io.netty.cases.chapter1.EchoExitServer2
 * @see io.netty.cases.chapter1.DaemonT1
 * @see io.netty.cases.chapter1.DaemonT2
 * @see io.netty.cases.chapter1.EchoExitServer3
 * @see io.netty.cases.chapter1.EchoExitServer4
 * @see io.netty.cases.chapter1.Shutdown1
 * @see io.netty.cases.chapter1.SignalHandlerTest
 *
 * 第二章
 * @see io.netty.cases.chapter2.MockServer 服务端，用于本章节的客户端测试
 * @see io.netty.cases.chapter2.ClientLeak
 * @see io.netty.cases.chapter2.ClientPool
 * @see io.netty.cases.chapter2.ClientPoolError1
 *
 * 第三章
 * @see io.netty.cases.chapter3.RouterClient 客户端，用于本章节的服务端测试
 * @see io.netty.cases.chapter3.RouterServerUnpooled
 * @see io.netty.cases.chapter3.RouterServerUnpooledV2
 * @see io.netty.cases.chapter3.PoolByteBufPerformanceTest
 *
 * 第四章
 * @see io.netty.cases.chapter4.HttpServer 服务端，用于本章节的客户端测试
 * @see io.netty.cases.chapter4.HttpClient
 * @see io.netty.cases.chapter4.HttpClient2
 * @see io.netty.cases.chapter4.HttpClient3
 *
 * 第五章
 * @see io.netty.cases.chapter5.EchoServer 服务端，用于本章节的客户端测试
 * @see io.netty.cases.chapter5.LoadRunnerClient
 * @see io.netty.cases.chapter5.LoadRunnerWaterClient
 * @see io.netty.cases.chapter5.LoadRunnerSleepClient 需要服务端配合
 *
 * 第六章
 * @see io.netty.cases.chapter6.ApiGatewayClient 客户端，用于本章节的服务端测试
 * @see io.netty.cases.chapter6.ApiGatewayServer 服务端，有内存泄漏的问题
 * @see io.netty.cases.chapter6.ApiGatewayServer2 服务端，解决内存泄漏
 *
 * 第七章
 * @see io.netty.cases.chapter7.ThreadSecurityServer 线程安全计数服务端，用于本章节的客户端测试
 * @see io.netty.cases.chapter7.ThreadSecurityClient 线程安全客户端，每个链路都有自己对应的业务 Handler 实例，不共享
 * @see io.netty.cases.chapter7.NoThreadSecurityClient 非线程安全客户端，每个链路共享了业务 Handler 实例，且业务 Handler 自己没有保证线程安全
 * @see io.netty.cases.chapter7.ThreadSecurityClient2 线程安全客户端，每个链路共享了业务 Handler 实例，但业务 Handler 自己保证了线程安全
 *
 * 第八章
 * @see io.netty.cases.chapter8.IotCarsClient 客户端，用于本章节的服务端测试
 * @see io.netty.cases.chapter8.IotCarsServer1 业务线程阻塞了 IO 线程
 * @see io.netty.cases.chapter8.IotCarsServer2 业务线程使用直接抛弃策略不会阻塞 IO 线程
 *
 * 第十章
 * @see io.netty.cases.chapter10.ConcurrentPerformanceClient 测试1：未优化的客户端
 * @see io.netty.cases.chapter10.ConcurrentPerformanceServer 测试1：未优化的服务端
 * @see io.netty.cases.chapter10.ConcurrentPerformanceServer 测试2：本类为优化后正确的服务端，可以直接和未优化的客户端产生正确的 qps
 * @see io.netty.cases.chapter10.MulChannelPerformanceClient 测试3：本类为优化后正确的客户端，可以直接和未优化的服务端产生正确的 qps
 *
 * 第十二章
 * @see io.netty.cases.chapter12.MockEdgeService 通过 Arrays.copyOf 拷贝的 Eden Space GC 例子
 * @see io.netty.cases.chapter12.MockEdgeService2 通过直接返回的 Eden Space GC 例子
 * @see io.netty.cases.chapter12.CloneTest 通过浅拷贝，导致引用重用了对象
 * @see io.netty.cases.chapter12.CloneTest2 通过深拷贝，独立的引用
 *
 * 第十三章
 * @see io.netty.cases.chapter13.ServiceTraceClient 客户端，用于本章节的服务端测试
 * @see io.netty.cases.chapter13.ServiceTraceServer 统计有问题的服务端
 * @see io.netty.cases.chapter13.ServiceTraceServerV2 正确统计了 qps、队列积压、线程池指标的服务端
 * @see io.netty.cases.chapter13.ServiceTraceServerV3 在 V2 基础上，统计了消息读取速度指标的服务端
 *
 * 第十五章
 * @see io.netty.cases.chapter15.EventTriggerClient 客户端，用于本章节的服务端测试
 * @see io.netty.cases.chapter15.EventTriggerServer 服务端，多次调用 channelReadComplete
 *
 * 第十六章
 * @see io.netty.cases.chapter16.TrafficShappingClient 客户端，用于本章节的服务端测试，恒定速率发送
 * @see io.netty.cases.chapter16.TrafficShappingServer 服务端，流量整形，恒定速率接受
 * @see io.netty.cases.chapter16.TrafficShappingClient2 异常客户端，由于没有判断发送时状态，导致队列阻塞造成 OOM
 *
 * 第十八章
 *
 */
package io.netty.cases;