package at.refugeescode.checkin.config;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String ADMIN_ROLE = "ADMIN";

    @Value("${checkin.auth.enabled}")
    private boolean authEnabled;
    @Value("${checkin.auth.username}")
    private String authUsername;
    @Value("${checkin.auth.password}")
    private String authPassword;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (authEnabled)
            http
                    .cors()
                    .and()
                    .httpBasic()
                    .and()
                    .authorizeRequests()
                    .antMatchers("/public/**")
                    .permitAll()
                    .anyRequest().hasRole(ADMIN_ROLE)
                    .and()
                    .csrf().disable();
        else
            http
                    .cors()
                    .and()
                    .csrf().disable();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (authEnabled)
            auth
                    .inMemoryAuthentication()
                    .withUser(authUsername).password(authPassword).roles(ADMIN_ROLE);
    }

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