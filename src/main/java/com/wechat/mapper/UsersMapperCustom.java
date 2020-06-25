package com.wechat.mapper;

import com.wechat.pojo.Users;
import com.wechat.pojo.vo.FriendsRequestVO;
import com.wechat.pojo.vo.MyFriendsVO;
import com.wechat.utils.MyMapper;
import java.util.List;


public interface UsersMapperCustom extends MyMapper<Users> {

    public List<FriendsRequestVO> queryFriendRequestList(String acceptUserId);

    public List<MyFriendsVO> queryMyFriends(String userId);

    public void batchUpdateMsgSigned(List<String> msgIdList);


}