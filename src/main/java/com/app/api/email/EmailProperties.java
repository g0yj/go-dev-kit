package com.app.api.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.mail") // ✅ `application.yml`의 `spring.mail` 값을 매핑
public class EmailProperties {
    private String host;
    private int port;
    private String username;
    private String password;

    private Smtp smtp = new Smtp();
    private Imap imap = new Imap();

    @Getter
    @Setter
    public static class Smtp {
        private boolean auth;
        private Starttls starttls = new Starttls();

        @Getter
        @Setter
        public static class Starttls {
            private boolean enable;
        }
    }

    @Getter
    @Setter
    public static class Imap {
        private Ssl ssl = new Ssl();

        @Getter
        @Setter
        public static class Ssl {
            private boolean enable;
        }
    }

}
