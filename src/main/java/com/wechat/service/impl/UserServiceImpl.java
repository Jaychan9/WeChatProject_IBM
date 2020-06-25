package com.wechat.service.impl;

import com.wechat.enums.MsgActionEnum;
import com.wechat.enums.MsgSignFlagEnum;
import com.wechat.enums.SearchFriendsStatusEnum;
import com.wechat.mapper.*;
import com.wechat.netty.DataContent;
import com.wechat.netty.UserChannalRel;
import com.wechat.pojo.FriendsRequest;
import com.wechat.pojo.MyFriends;
import com.wechat.pojo.Users;
import com.wechat.pojo.vo.FriendsRequestVO;
import com.wechat.pojo.vo.MyFriendsVO;
import com.wechat.service.UserService;
import com.wechat.utils.FastDFSClient;
import com.wechat.utils.FileUtils;
import com.wechat.utils.JsonUtils;
import com.wechat.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;
import com.wechat.netty.ChatMsg;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;


/**
 * @author Jaychan
 * @date 2020/6/8
 * @description TODO
 */

@Service
public class UserServiceImpl implements UserService {


    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Autowired
    private ChatMsgMapper chatMsgMapper;




    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {

        Users users = new Users();
        users.setUsername(username);
        Users result = usersMapper.selectOne(users);

        return result !=null ? true:false;
    }



    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String pwd) {

        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();

        criteria.andEqualTo("username",username);
        criteria.andEqualTo("password",pwd);

        Users result = usersMapper.selectOneByExample(userExample);

        return result;
    }

    @Override
    public Users saveUser(Users user) {
        String userId = UUID.randomUUID().toString();


        //wechat_qrcode:[username]



        /*生成二维码*/
        String qrCodePath="C:\\wechat-qrcode-upload\\user"+userId+"qrcode.png";
        System.out.println(qrCodePath);
        qrCodeUtils.createQRCode(qrCodePath,"wechat_qrcode:"+user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);

        String qrCodeUrl="";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);

        user.setId(userId);
        usersMapper.insert(user);



        return user;
    }


                                        //必须要存在一个事务
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
        //只会更新user原来的数据
        usersMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());
    }


    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {

        //前置条件 - 1 搜索用户不存在，返回[无此用户]
        Users user = null;
        user = queryUserInfoByUsername(friendUsername);
        if (user == null) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        //前置条件 - 2 搜索用户是自己，返回[不能添加自己]
        if (StringUtils.equals(myUserId,user.getId())) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        //前置条件 - 3 搜索用户已经是好友，返回[已经是好友]
        Example mfe = new Example(MyFriends.class);
        Example.Criteria mfc = mfe.createCriteria();
        mfc.andEqualTo("myUserId",myUserId);
        mfc.andEqualTo("myFriendUserId",user.getId());
        MyFriends myFriendRel = myFriendsMapper.selectOneByExample(mfe);
        if (myFriendRel !=null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }

        return SearchFriendsStatusEnum.SUCCESS.status;
    }



    /*根据用户id查user*/
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username){

        Example ue = new Example(Users.class);
        Example.Criteria uc = ue.createCriteria();
        uc.andEqualTo("username",username);
        return usersMapper.selectOneByExample(ue);

    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

        //限制多次添加，只一次

        //根据用户名查询朋友信息
        Users friend = queryUserInfoByUsername(friendUsername);


        //1.查询发送好友请求记录表
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId",myUserId);
        frc.andEqualTo("acceptUserId",friend.getId());
        FriendsRequest friendRequest = friendsRequestMapper.selectOneByExample(fre);
        if (friendRequest == null) {

            //2.如果不是好友，并好有记录也没有添加，新增好友请求记录

            String requestId = UUID.randomUUID().toString();

            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setAcceptUserId(friend.getId());
            request.setSendUserId(myUserId);
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
//            System.out.println("好友请求发送成功了");

        }


    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendsRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }



    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId",sendUserId);
        frc.andEqualTo("acceptUserId",acceptUserId);
        friendsRequestMapper.deleteByExample(fre);

    }



    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        /*通过添加好友记录
         *       1.保存好友
         *       2.逆向保存好友
         *       3.删除好友请求记录
         * */
        saveFriends(sendUserId,acceptUserId);
        saveFriends(acceptUserId,sendUserId);
        deleteFriendRequest(sendUserId,acceptUserId);


        Channel sendChannel = UserChannalRel.get(sendUserId);
        if (sendChannel != null) {
            //使用websocket主动推送消息到请求发起者，更新它的通讯录
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }



    }



    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {

        List<MyFriendsVO> myFriendsVOS = usersMapperCustom.queryMyFriends(userId);

        return myFriendsVOS;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {

        com.wechat.pojo.ChatMsg msgDB = new com.wechat.pojo.ChatMsg();
        String msgId = UUID.randomUUID().toString();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());
        chatMsgMapper.insert(msgDB);
        return msgId;


    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {

        usersMapperCustom.batchUpdateMsgSigned(msgIdList);



    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.wechat.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {

        Example chatExample = new Example(com.wechat.pojo.ChatMsg.class);
        Example.Criteria criteria = chatExample.createCriteria();

        criteria.andEqualTo("signFlag",0);
        criteria.andEqualTo("acceptUserId",acceptUserId);

        List<com.wechat.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);


        return result;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    void saveFriends(String sendUserId, String acceptUserId){

        MyFriends myFriends = new MyFriends();
        String recordId = UUID.randomUUID().toString();

        myFriends.setId(recordId);
        myFriends.setMyUserId(sendUserId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriendsMapper.insert(myFriends);


    }





    @Transactional(propagation = Propagation.SUPPORTS)
    public Users queryUserById(String userId){
        return usersMapper.selectByPrimaryKey(userId);

    }

}
