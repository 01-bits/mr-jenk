package com.api.media_service.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.api.media_service.model.Role;
import com.api.media_service.security.HeaderAuthFilter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * SecurityConfig
 */
@Configuration
@EnableMongoAuditing
public class SecurityConfig {

	@Bean
	SecurityFilterChain chain(HttpSecurity http,
			@Value("${auth.user-id-header}") String userIdHeader,
			@Value("${auth.user-role-header}") String roleHeader) throws Exception {

		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/media/images", "/media/images/*").permitAll()
						.requestMatchers(HttpMethod.POST, "/media/images").hasRole(Role.SELLER.name())
						.requestMatchers(HttpMethod.DELETE, "/media/images", "/media/images/*")
						.hasRole(Role.SELLER.name())
						.requestMatchers("/v3/api-docs/**").permitAll()
						.anyRequest().denyAll())
				.addFilterBefore(new HeaderAuthFilter(userIdHeader, roleHeader),
						UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(e -> e
						.authenticationEntryPoint(
								(req, res, ex) -> json(res, 401, "Unauthorized", "Authentication required"))
						.accessDeniedHandler((req, res, ex) -> json(res, 403, "Forbidden",
								"Access denied")))
				.build();
	}

	private static void json(HttpServletResponse res, int status, String error, String msg) throws IOException {
		res.setStatus(status);
		res.setContentType("application/json");
		res.getWriter().write(
				"{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + msg + "\"}");
	}
}