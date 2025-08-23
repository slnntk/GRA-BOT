package com.gra.paradise.botattendance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BotAttendanceApplicationTests {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully with H2 database
        // This validates that our database configuration works
        // Discord config is disabled in test profile
    }

}
