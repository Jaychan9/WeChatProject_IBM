package com.wechat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jaychan
 * @date 2020/6/8
 * @description TODO
 */

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello(){
        return "Hello Wechat";
    }



}
