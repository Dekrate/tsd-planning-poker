package pl.xsd.pokertable.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.xsd.pokertable.developer.DeveloperRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	private final DeveloperRepository developerRepository;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	public SecurityConfiguration(DeveloperRepository developerRepository, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
		this.developerRepository = developerRepository;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/developers/register", "/developers/login", "/error").permitAll()
						.anyRequest().authenticated()
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return email -> developerRepository.findByEmail(email)
				.map(developer -> User.withUsername(developer.getEmail())
						.password(developer.getPassword())
						.roles("DEVELOPER")
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Developer not found with email: " + email));
	}
}
