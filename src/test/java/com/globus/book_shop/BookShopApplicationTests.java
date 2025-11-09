package com.globus.book_shop;

import com.globus.book_shop.config.TestCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
class BookShopApplicationTests {

	@Test
	void contextLoads() {
	}

}
