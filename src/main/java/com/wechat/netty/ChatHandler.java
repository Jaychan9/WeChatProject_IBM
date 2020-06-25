package com.wechat.netty;

import com.wechat.SpringUtil;
import com.wechat.enums.MsgActionEnum;
import com.wechat.service.UserService;
import com.wechat.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Jaychan
 * @date 2020/6/7
 * @description 处理消息的handler
 */


/**
 * TextWebSocketFrame：在Netty中，是用于为websocket专门处理文本的对象，frame是消息的载体
 */

public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    //用于记录和管理所有客户端的channel
    public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        //获取从客户端传输过来的消息（字符串
        String content = msg.text();
        //System.out.println("服务器端接收到消息"+content);

        Channel channel = ctx.channel();
        //1.获取客户端发来的消息
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();
        //2.判断消息类型，根据不同类型来处理不同业务
        if (action == MsgActionEnum.CONNECT.type) {
            //  2.1 当websocket第一次open的时候，初始化channel，把用的channel和userid关联起来
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannalRel.put(senderId,channel);


            for (Channel c:users){
                System.out.println(c.id().asLongText());

            }
            UserChannalRel.output();



        } else if (action == MsgActionEnum.CHAT.type) {
            //  2.2聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态[未签收]
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgText = chatMsg.getMsg();
            String receiverId = chatMsg.getReceiverId();
            String senderId = chatMsg.getSenderId();

            //保存消息到数据库 并标记为未签收              //默认名
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);


            //发送消息
            //从全局用户channel关系中获取接收方的channel
            Channel receiverChannel = UserChannalRel.get(receiverId);
            if (receiverChannel == null) {
                //channel为空代表用户离线，推送消息 Jpush 个推 小米推送

            }else {
                //不为空，从channelgroup查找对应channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null) {
                    receiverChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContentMsg)));
                }else {
                    //用户离线

                }
            }

        }else if (action == MsgActionEnum.SIGNED.type) {
            //  2.3签收消息类型，针对具体消息进行签收，修改数据库中对应消息的签收状态[已签收]
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            // 扩展字段在signed类型的消息中，代表需要去签收的消息id，逗号间隔
            String msgIdsStr = dataContent.getExtand();
            String msgIds[] = msgIdsStr.split(",");

            List<String> msgIdlist = new ArrayList<>();
            for (String mid : msgIds){

                if (StringUtils.isNotBlank(mid)) {
                    msgIdlist.add(mid);
                }

            }
            System.out.println(msgIdlist.toString());
            if (msgIdlist != null&& !msgIdlist.isEmpty() && msgIdlist.size()>0) {
                //批量签收

                userService.updateMsgSigned(msgIdlist);


            }


        }else if (action == MsgActionEnum.KEEPALIVE.type) {
            //  2.4 心跳类型的消息
            System.out.println("收到来自channel为[" +channel+ "]的心跳包");

        }















/*        System.out.println("接收到的数据："+content);
        for (Channel channel : users){
            //不能是string 因为是以TextWebSocketFrame为载体
            channel.writeAndFlush(
                    new TextWebSocketFrame(
                            "[服务器接收到消息]"+LocalDateTime.now()
                                    +"接收到消息,消息为："+content));
        }*/

        //等于上面的for循环
/*
        clients.writeAndFlush("[服务器在：]"
                +LocalDateTime.now()+"接收到消息，消息为："
                +content);
*/


    }


    /**
     * 当客户端获取服务端链接之后（打开链接）
     * 获取客户端的channel，并放入channelgroup中去管理
     */

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

        users.add(ctx.channel());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //发送异常之后关闭连接（关闭channel），随后从channelgroup中移除
        ctx.channel().close();
        users.remove(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //当触发handlerremove，chiannelgroup会自动移除对应客户端的channel
          users.remove(ctx.channel());

    }




}
