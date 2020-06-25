package com.wechat.netty;

import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;


/**
 * @author Jaychan
 * @date 2020/6/7
 * @description 继承ChannelInboundHandlerAdapter，从而不需要事先channelRead0方法
 *              用于检测channel心跳的handler
 *
 */



public class HeartBeatHandler extends ChannelInboundHandlerAdapter {



    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        //判断evt是否是idlestateevent（用于触发用户时间，包含读空闲、写空闲、读写空闲）
        if(evt instanceof IdleStateEvent){


            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state() == IdleState.READER_IDLE) {

                System.out.println("进入读空闲");


            } else if(event.state() == IdleState.WRITER_IDLE){
                System.out.println("进入写空闲");
            }else if(event.state() == IdleState.ALL_IDLE){

                System.out.println("进入读写空闲");
                System.out.println("channel关闭前，users的数量为:"+ChatHandler.users.size());
                Channel channel = ctx.channel();

                //关闭无用的channel，以防资源浪费
                channel.close();
                System.out.println("channel关闭后，users的数量为:"+ChatHandler.users.size());
            }

        }


    }
}
