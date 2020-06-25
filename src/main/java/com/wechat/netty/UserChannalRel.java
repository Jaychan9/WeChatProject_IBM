package com.wechat.netty;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jaychan
 * @date 2020/6/13
 * @description 用户id和channel的关联关系处理
 */
public class UserChannalRel {

    private static HashMap<String, Channel> manager = new HashMap<>();


    public static void put(String senderId, Channel channal){
        manager.put(senderId,channal);


    }

    public static Channel get(String senderId){
        return manager.get(senderId);
    }



    public static void output(){


        for (Map.Entry<String,Channel> entry:manager.entrySet()){
            System.out.println("userId:"+entry.getKey() + ",channelid"+entry.getValue().id().asLongText());
        }

    }





}
