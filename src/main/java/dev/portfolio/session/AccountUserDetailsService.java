package dev.portfolio.session;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

	private final UserAccountRepository users;

	public AccountUserDetailsService(UserAccountRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		UserAccount account = users.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException(email));
		return User.withUsername(account.getEmail())
				.password(account.getPasswordHash())
				.roles("MEMBER")
				.build();
	}
}
