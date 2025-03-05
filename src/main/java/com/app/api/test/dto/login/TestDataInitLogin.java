package com.app.api.test.dto.login;

import com.app.api.login.UserType;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

@Configuration
public class TestDataInitLogin {

    @Bean
    public CommandLineRunner iniData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 🔹 테이블이 존재하는지 확인 후 실행
            if (!isTableExists("users")) {
                System.out.println("🚨 users 테이블이 존재하지 않음. 초기 데이터 삽입 생략.");
                return;
            }

            // 🔹 중복 저장 방지 (이미 존재하는 경우 저장하지 않음)
            if (!userRepository.existsByUsername("adminA")) {
                userRepository.save(new UserEntity(null, "adminA", passwordEncoder.encode("a1234"), UserType.A));
            }
            if (!userRepository.existsByUsername("bossB")) {
                userRepository.save(new UserEntity(null, "bossB", passwordEncoder.encode("b1234"), UserType.B));
            }
            if (!userRepository.existsByUsername("customerC")) {
                userRepository.save(new UserEntity(null, "customerC", passwordEncoder.encode("c1234"), UserType.C));
            }

            System.out.println("✅ 초기 데이터 삽입 완료!");
        };
    }

    // ✅ 🔹 `CommandLineRunner` 내부가 아니라, 클래스 내부에서 메서드 정의
    private boolean isTableExists(String tableName) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/goapp", "root", "1234");
             ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
