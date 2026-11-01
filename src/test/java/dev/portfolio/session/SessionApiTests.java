package dev.portfolio.session;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@SpringBootTest
@AutoConfigureMockMvc
class SessionApiTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void signupWithoutCsrfIsRejected() throws Exception {
		mvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(account("ada@example.com")))
				.andExpect(status().isForbidden());
	}

	@Test
	void signupCreatesASessionAndMeRequiresIt() throws Exception {
		MvcResult signup = mvc.perform(post("/api/auth/signup")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(account("ada@example.com")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("ada@example.com"))
				.andReturn();

		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

		Cookie session = signup.getResponse().getCookie("SESSION");
		mvc.perform(get("/api/auth/me").cookie(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ada"));
	}

	@Test
	void wrongPasswordIsUnauthorized() throws Exception {
		mvc.perform(post("/api/auth/signup")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(account("bea@example.com")))
				.andExpect(status().isCreated());

		mvc.perform(post("/api/auth/signin")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"bea@example.com\",\"password\":\"not-the-password\"}"))
				.andExpect(status().isUnauthorized());
	}

	private static String account(String email) {
		return "{\"name\":\"Ada\",\"email\":\"" + email + "\",\"password\":\"correct-horse-1\"}";
	}
}
