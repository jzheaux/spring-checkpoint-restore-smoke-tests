package com.example.security.webflux;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {

	@Bean
	public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
		return http
			.authorizeExchange((exchanges) -> exchanges.pathMatchers("/rest/anonymous")
				.permitAll()
				.pathMatchers("/rest/admin")
				.hasRole("ADMIN")
				.anyExchange()
				.authenticated())
			.httpBasic(Customizer.withDefaults())
			.build();
	}

	@Bean
	public ReactiveUserDetailsService userDetailsService() {
		UserDetails user = User.withDefaultPasswordEncoder()
			.username("user")
			.password("password")
			.roles("USER")
			.build();
		UserDetails admin = User.withDefaultPasswordEncoder()
			.username("admin")
			.password("password")
			.roles("ADMIN")
			.build();

		return new MapReactiveUserDetailsService(user, admin);
	}

}
