package com.cst438;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cst438.services.RegistrationService;
import com.cst438.services.RegistrationServiceMQ;
import com.cst438.services.RegistrationServiceREST;

@SpringBootApplication
@EnableWebSecurity
public class Cst438GradebookApplication extends WebSecurityConfigurerAdapter  {

	public static void main(String[] args) {
		SpringApplication.run(Cst438GradebookApplication.class, args);
	}
	
	@Bean(name = "RegistrationService")
	@ConditionalOnProperty(prefix = "registration", name = "service", havingValue = "MQ")
	public RegistrationService registrationServiceRESTMQ() {
		return new RegistrationServiceMQ();
	}
	
	
	@Bean(name = "RegistrationService")
	@ConditionalOnProperty(prefix = "registration", name = "service", havingValue = "REST")
	public RegistrationService registrationServiceREST() {
		return new RegistrationServiceREST();
	}
	
	@Bean(name = "RegistrationService")
	@ConditionalOnProperty(prefix = "registration", name = "service", havingValue = "default")
	public RegistrationService registrationServiceDefault() {
		return new RegistrationService();
	}
	
	@Override
   	protected void configure(HttpSecurity http) throws Exception {
		SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler("/");
		http.cors();
 		http.csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
 		// permit requests to /course without authentication. All other URLS are authenticated
 		http.authorizeRequests().mvcMatchers(HttpMethod.PUT, "/enrollment").permitAll();
 		http.antMatcher("/**").authorizeRequests( a -> a.antMatchers("/", "/home", "/login", "/webjars/**").permitAll()
 		.anyRequest().authenticated())
 		.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
		.logout(l -> l.logoutSuccessUrl("/").permitAll() )
 		.oauth2Login(o -> o.failureHandler((request, response, exception) -> {
 			System.out.println("error.message " + exception.getMessage());
 			handler.onAuthenticationFailure(request, response, exception);
 		}));
	}
	

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://accounts.google.com"));
		config.setAllowedMethods(Arrays.asList("*"));
		config.setAllowedHeaders(Arrays.asList("*"));
		config.setAllowCredentials(true);
		config.applyPermitDefaultValues();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

}
