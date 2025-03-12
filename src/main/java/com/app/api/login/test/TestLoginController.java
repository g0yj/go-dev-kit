package com.app.api.login.test;

import lombok.Getter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestLoginController {
    @GetMapping("/")
    public String main(){
        return "index";
    }


    @GetMapping("/user/main")
    public String userMainPage(Model model) {
        // 필요하면 여기에 사용자 데이터를 추가할 수 있음
        return "user/main"; // `user/main.html` Thymeleaf 템플릿을 반환
    }

    @GetMapping("/admin/main")
    public String adminMainPage(Model model) {
        // 필요하면 여기에 사용자 데이터를 추가할 수 있음
        return "admin/main"; // `user/main.html` Thymeleaf 템플릿을 반환
    }

}
