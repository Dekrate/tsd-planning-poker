package pl.xsd.pokertable.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedMethods("*")
				.allowedOrigins("http://localhost:5173",
						"https://dekrate.github.io/tsd-planning-poker/")
				.allowCredentials(true);
	}
}
