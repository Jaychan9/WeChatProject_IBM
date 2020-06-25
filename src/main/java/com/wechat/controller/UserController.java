package com.wechat.controller;

import com.wechat.enums.OperatorFriendRequestTypeEnum;
import com.wechat.enums.SearchFriendsStatusEnum;
import com.wechat.mapper.UsersMapper;
import com.wechat.pojo.ChatMsg;
import com.wechat.pojo.Users;
import com.wechat.pojo.bo.UsersBO;
import com.wechat.pojo.vo.MyFriendsVO;
import com.wechat.pojo.vo.UsersVO;
import com.wechat.service.UserService;
import com.wechat.utils.FastDFSClient;
import com.wechat.utils.FileUtils;
import com.wechat.utils.IMoocJSONResult;
import com.wechat.utils.MD5Utils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * @author Jaychan
 * @date 2020/6/8
 * @description TODO
 */

@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserService userService;


    @Autowired
    private FastDFSClient fastDFSClient;


    @Autowired
    private UsersMapper usersMapper;




    @PostMapping("/registerOrLogin")
    public IMoocJSONResult registerOrLogin(@RequestBody Users user) throws Exception {

//        System.out.println("终于进来了");


        String s = user.toString();
        System.out.println(s);

        //判断用户名和密码不能为空
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return  IMoocJSONResult.errorMap("用户名或密码不能为空");
        }

        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());

        Users userResult = null;
        if (usernameIsExist) {
//            登录
            userResult = userService.queryUserForLogin(user.getUsername(),MD5Utils.getMD5Str(user.getPassword()));
            if (userResult == null) {
                return IMoocJSONResult.errorMsg("用户名或密码不正确");
            }

        }else {
//            注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);

        }


        UsersVO usersVO = new UsersVO();

        BeanUtils.copyProperties(usersVO,userResult);

        return IMoocJSONResult.ok(usersVO);
    }




    @PostMapping("/uploadFaceBase64")
    public IMoocJSONResult uploadFaceBase64(@RequestBody UsersBO usersBO) throws Exception{

        //获取前端传过来的base64字符串，然后转换为文件对象再上传

        String base64Data = usersBO.getBase64Data();

        String userFacePath = "C:\\wechat-avatar-upload\\" +usersBO.getUserId()+ "userface64.png";

        //写到本地
        FileUtils.base64ToFile(userFacePath,base64Data);


        //转化为fastdfsclient能传的类型 MultipartFile
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);

        String url = fastDFSClient.uploadBase64(faceFile);

        System.out.println(url);

        String thump = "_80x80.";


        // M00/00/00/wKh7e17gvHaAfWvBAArK9YSdTm8534.png
        String[] arr = url.split("\\.");
        String thumpImgUrl = arr[0] +thump + arr[1];
//        System.out.println(url);
//        System.out.println(thumpImgUrl);



        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);


        Users result = userService.updateUserInfo(user);


        return IMoocJSONResult.ok(result);
    }


    @PostMapping("/setNickname")
    public IMoocJSONResult setNickname(@RequestBody UsersBO usersBO) throws Exception{

        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setNickname(usersBO.getNickname());


        Users result = userService.updateUserInfo(user);


        return IMoocJSONResult.ok(result);
    }



    /*搜索好友接口,根据账号做匹配查询而不是模糊查询
    * */
    @PostMapping("/search")
    public IMoocJSONResult searchUser(String myUserId,String friendUsername) throws Exception{

        if(StringUtils.isBlank(myUserId)||StringUtils.isBlank(friendUsername)){

            return IMoocJSONResult.errorMsg("");

        }


        /*
        * 前置条件 - 1 搜索用户不存在，返回[无此用户]
        * 前置条件 - 2 搜索用户是自己，返回[不能添加自己]
        * 前置条件 - 3 搜索用户已经是好友，返回[已经是好友]
        * */



        Integer status = userService.preconditionSearchFriends(myUserId,friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(usersVO,user);
            return IMoocJSONResult.ok(usersVO);


        }else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }



    }




    /*
     *  添加好友接口
     */
    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId,String friendUsername) throws Exception{

        if(StringUtils.isBlank(myUserId)||StringUtils.isBlank(friendUsername)){

            return IMoocJSONResult.errorMsg("");

        }

        /*
         * 前置条件 - 1 搜索用户不存在，返回[无此用户]
         * 前置条件 - 2 搜索用户是自己，返回[不能添加自己]
         * 前置条件 - 3 搜索用户已经是好友，返回[已经是好友]
         * */

        Integer status = userService.preconditionSearchFriends(myUserId,friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId,friendUsername);

        }else {

            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);

        }


            return IMoocJSONResult.ok();
    }



    /*查询好友请求*/
    @PostMapping("/queryFriendsRequest")
    public IMoocJSONResult queryFriendsRequest(String userId) throws Exception {

        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }

        /*查询用户接收到的朋友申请*/
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));



    }

    /*接收方 通过或者忽略好友请求*/
    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId,String sendUserId,Integer operType) throws Exception {

        //传入参数不能为空
        if (StringUtils.isBlank(acceptUserId)
                || StringUtils.isBlank(sendUserId)
                || operType == null) {
            return IMoocJSONResult.errorMsg("");
        }


        //操作类型没有枚举值，也抛出空错误信息
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IMoocJSONResult.errorMsg("");
        }


        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            //如果忽略，直接删除记录
            userService.deleteFriendRequest(sendUserId,acceptUserId);

        } else if (operType == OperatorFriendRequestTypeEnum.PASS.type) {

            //添加好友到列表数据库
            userService.passFriendRequest(sendUserId,acceptUserId);

        }

        List<MyFriendsVO> myFriendsVOS = userService.queryMyFriends(acceptUserId);

        return  IMoocJSONResult.ok(myFriendsVOS);
    }



    /*查询我的好友列表*/
    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId) throws Exception {

        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }

        List<MyFriendsVO> myFriendsVOS = userService.queryMyFriends(userId);

        return IMoocJSONResult.ok(myFriendsVOS);
    }




    @PostMapping("/getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String accpetUserId) throws Exception {

        if (StringUtils.isBlank(accpetUserId)) {
            return IMoocJSONResult.errorMsg("");
        }

        //查询列表
        List<ChatMsg> unReadMsgList = userService.getUnReadMsgList(accpetUserId);

        return IMoocJSONResult.ok(unReadMsgList);
    }




}
