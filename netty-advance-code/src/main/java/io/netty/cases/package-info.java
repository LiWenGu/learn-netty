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
 */
package io.netty.cases;