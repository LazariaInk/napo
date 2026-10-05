package com.lazar.napo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class NapoApplicationTests {

	@Test
	void applicationClassIsSpringBootApplication() {
		assertThat(NapoApplication.class).hasAnnotation(SpringBootApplication.class);
	}

}
