package io.netty.cases.chapter12;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;

import java.util.concurrent.TimeUnit;

/**
 * Created by 李林峰 on 2018/8/27.
 * Updated by liwenguang on 2020/05/14.
 */
public class MockEdgeService {

    public static void main(String[] args) throws Exception {
        testCopyHotMethod();
    }

    static void testCopyHotMethod() throws Exception {
        ByteBuf buf = Unpooled.buffer(1024);
        for (int i = 0; i < 1024; i++) {
            buf.writeByte(i);
        }
        RestfulReq req = new RestfulReq(buf.array());
        while (true) {
            byte[] msgReq = req.body();
            TimeUnit.MICROSECONDS.sleep(1);
        }
    }
}
