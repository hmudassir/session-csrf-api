package dev.portfolio.session;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserAccountRepository users;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository;

	public AuthController(
			UserAccountRepository users,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping("/csrf")
	public CsrfView csrf(CsrfToken token) {
		return new CsrfView(token.getToken());
	}

	@PostMapping("/signup")
	public org.springframework.http.ResponseEntity<UserView> signup(
			@Valid @RequestBody SignupRequest request,
			HttpServletRequest http,
			HttpServletResponse response) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmailIgnoreCase(email)) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email is already registered");
			problem.setTitle("Conflict");
			throw new org.springframework.web.ErrorResponseException(HttpStatus.CONFLICT, problem, null);
		}
		users.save(new UserAccount(email, passwordEncoder.encode(request.password()), request.name().trim()));
		establishSession(email, request.password(), http, response);
		return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED)
				.body(new UserView(email, request.name().trim()));
	}

	@PostMapping("/signin")
	public UserView signin(
			@Valid @RequestBody SigninRequest request,
			HttpServletRequest http,
			HttpServletResponse response) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		establishSession(email, request.password(), http, response);
		UserAccount account = users.findByEmailIgnoreCase(email).orElseThrow();
		return new UserView(account.getEmail(), account.getName());
	}

	@PostMapping("/logout")
	public org.springframework.http.ResponseEntity<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return org.springframework.http.ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public UserView me(Authentication authentication) {
		UserAccount account = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
		return new UserView(account.getEmail(), account.getName());
	}

	private void establishSession(
			String email,
			String password,
			HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, password));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}

	public record SignupRequest(
			@NotBlank @Size(max = 120) String name,
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(min = 12, max = 128) String password) {
	}

	public record SigninRequest(
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(max = 128) String password) {
	}

	public record UserView(String email, String name) {
	}

	public record CsrfView(String token) {
	}
}
