package at.refugeescode.checkin.config;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String CLIENT_ROLE = "CLIENT";

    @Value("${checkin.auth.username}")
    private String username;
    @Value("${checkin.auth.password}")
    private String password;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
//                .httpBasic()
//                .and()
//                .authorizeRequests()
//                .anyRequest().hasRole(CLIENT_ROLE)
//                .and()
                .csrf().disable();
    }

//    @Autowired
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth
//                .inMemoryAuthentication()
//                .withUser(username).password(password).roles(CLIENT_ROLE);
//    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ImmutableList.of(CorsConfiguration.ALL));
        configuration.setAllowedMethods(ImmutableList.of(CorsConfiguration.ALL));
        configuration.setAllowedHeaders(ImmutableList.of(CorsConfiguration.ALL));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}