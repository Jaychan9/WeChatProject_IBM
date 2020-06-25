package com.wechat.netty;


import java.io.Serializable;

/**
 * @author Jaychan
 * @date 2020/6/13
 * @description TODO
 */
public class DataContent implements Serializable {


    private static final long serialVersionUID = 6889362435884394721L;

    //动作类型
    private Integer action;

    //用户的聊天内容entitiy
    private ChatMsg chatMsg;

    //扩展字段
    private String extand;

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public ChatMsg getChatMsg() {
        return chatMsg;
    }

    public void setChatMsg(ChatMsg chatMsg) {
        this.chatMsg = chatMsg;
    }

    public String getExtand() {
        return extand;
    }

    public void setExtand(String extand) {
        this.extand = extand;
    }







}
