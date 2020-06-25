package com.wechat.service;

import com.wechat.netty.ChatMsg;
import com.wechat.pojo.Users;
import com.wechat.pojo.vo.FriendsRequestVO;
import com.wechat.pojo.vo.MyFriendsVO;

import java.util.List;


/**
 * @author Jaychan
 * @date 2020/6/8
 * @description TODO
 */

public interface UserService {

    /*判断用户名是否存在*/
    public boolean queryUsernameIsExist(String username);


    /*查询用户是否存在*/
    public Users queryUserForLogin(String username,String pwd);

    /*保存用户*/
    public Users saveUser(Users user);


    /*修改用户记录*/

    public Users updateUserInfo(Users user);


    /*搜索朋友的前置条件*/
    public Integer preconditionSearchFriends(String myUserId,String friendUsername);


    /*根据username查询user*/
    public Users queryUserInfoByUsername(String username);


    /*发送好友请求 保存到数据库*/
    void sendFriendRequest(String myUserId, String friendUsername);

    /*查询好友请求*/
    public List<FriendsRequestVO> queryFriendRequestList(String acceptUserId);


    /*删除添加好友记录*/
    public void deleteFriendRequest(String sendUserId,String acceptUserId);


    /*通过添加好友记录
    *       1.保存好友
    *       2.逆向保存好友
    *       3.删除好友请求记录
    * */

    public void passFriendRequest(String sendUserId,String acceptUserId);



    /*查询好友列表*/
    public List<MyFriendsVO> queryMyFriends(String userId);


    /*保存聊天消息到db*/
    public String saveMsg(ChatMsg chatMsg);



    /*批量签收消息*/
    public void updateMsgSigned(List<String> msgIdList);

    /*获取未签收消息列表*/
    public List<com.wechat.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);

}
