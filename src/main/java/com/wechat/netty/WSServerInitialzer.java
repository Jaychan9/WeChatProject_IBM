package com.wechat.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author Jaychan
 * @date 2020/6/7
 * @description TODO
 */
public class WSServerInitialzer extends ChannelInitializer<SocketChannel> {


    protected void initChannel(SocketChannel socketChannel) throws Exception {

        ChannelPipeline pipeline = socketChannel.pipeline();

        //websocket基于http协议，所以要有http编解码器
        pipeline.addLast("httpservercodec", new HttpServerCodec());

        //大数据流handler
        pipeline.addLast(new ChunkedWriteHandler());

        //对httpmeassage进行聚合，聚合成fullhttprequest或fullhttpresponse
        //几乎在netty中的编程，都会用到此handler
        pipeline.addLast(new HttpObjectAggregator(1024 * 64));



        /*=================================以上是用于http协议========================*/


        /*=================================增加心跳支持 start========================*/

        //针对客户端，如果在1分钟时，没有向服务端发送心跳（ALL），则主动断开
        //如果是读空闲、写空闲 不作处理
        pipeline.addLast(new IdleStateHandler(8,10,12));
        //自定义空闲状态检测
        pipeline.addLast(new HeartBeatHandler());


        /*=================================增加心跳支持 end========================*/



/*        添加对websocket的支持
        websock服务器处理的协议，用于指定给客户端链接访问的路由 ：/ws
        本handler会帮我处理一些繁杂的事
        会帮我处理一些握手动作：handshaking(close,ping,pong) ping+pong=心跳
        对于websocket来说，都是以frames进行传输的，不同的数据类型对应的frames也不同*/

        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        //自定义handler
        pipeline.addLast(new ChatHandler());





    }
}
