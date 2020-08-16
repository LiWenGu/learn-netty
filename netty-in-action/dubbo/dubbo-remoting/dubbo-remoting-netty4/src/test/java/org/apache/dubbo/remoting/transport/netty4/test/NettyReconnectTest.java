package org.apache.dubbo.remoting.transport.netty4.test;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.Replier;
import org.junit.jupiter.api.Test;

public class NettyReconnectTest {

    @Test
    public void testCase() throws InterruptedException, RemotingException {
        testReconnect();
    }

    public void testReconnect() throws RemotingException, InterruptedException {

        {
            int port = 63491;
            Client client = startClient(port, 500);
            System.out.println("没有连接，这是正常的" + client.isConnected());
            ExchangeServer server = startServer(port);
            for (int i = 0; i < 3 && !client.isConnected(); i++) {
                Thread.sleep(1000);
            }
            System.out.println("有连接，这是正常的" + client.isConnected());
            client.close(1500);
            server.close(1500);
        }
    }

    public Client startClient(int port, int reconnectPeriod) throws RemotingException {
        final String url = "exchange://127.0.0.1:" + port + "/client.reconnect.test?check=false&transporter=netty4&reconnect.waring.period=3000";
        ;
        return Exchangers.connect(url);
    }

    public ExchangeServer startServer(int port) throws RemotingException {
        final URL url = URL.valueOf("exchange://127.0.0.1:" + port + "/client.reconnect.test?transporter=netty4&reconnect.waring.period=3000");
        return Exchangers.bind(url, new Replier<Object>() {
            @Override
            public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
                return request;
            }
        });
    }


}
