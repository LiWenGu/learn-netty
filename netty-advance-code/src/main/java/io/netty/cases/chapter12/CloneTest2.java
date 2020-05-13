package io.netty.cases.chapter12;

/**
 * Created by 李林峰 on 2018/8/27.
 * Updated by liwenguang on 2020/05/14.
 */
public class CloneTest2 {

    public static void main(String[] args) throws Exception {
        testCloneV2();
    }

    static void testCloneV2() throws Exception {
        UserInfoV2 user = new UserInfoV2();
        user.setAge(10);
        Address address = new Address();
        address.setCity("BeiJing");
        user.setAddress(address);
        System.out.println("The old value is : " + user);
        UserInfoV2 cloneUser = (UserInfoV2) user.clone();
        cloneUser.setAge(20);
        cloneUser.getAddress().setCity("NanJing");
        System.out.println("The new value is : " + user);
    }
}
