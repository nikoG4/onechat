package com.chat.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api")
public class BaseController {


    @GetMapping("check")
    public String test() throws IOException {
        return "ok";
    }

}
