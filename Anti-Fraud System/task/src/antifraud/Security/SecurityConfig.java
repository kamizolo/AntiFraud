package antifraud.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {


        return http
                .httpBasic(Customizer.withDefaults())
                .csrf().disable()                           // For modifying requests via Postman
               /* .exceptionHandling(handing -> handing
                        .authenticationEntryPoint(restAuthenticationEntryPoint) // Handles auth error
                )*/
                .headers(headers -> headers.frameOptions().disable())           // for Postman, the H2 console
                .authorizeHttpRequests(requests -> requests                     // manage access

                        .antMatchers(HttpMethod.POST, "/api/auth/user").permitAll()

                        .antMatchers(HttpMethod.PUT, "/api/auth/role/**").hasRole("ADMINISTRATOR")
                        .antMatchers(HttpMethod.PUT, "/api/auth/access/**").hasRole("ADMINISTRATOR")
                        .antMatchers(HttpMethod.DELETE, "/api/auth/user/*").hasRole("ADMINISTRATOR")
                        .antMatchers(HttpMethod.GET, "/api/auth/list").hasAnyRole("ADMINISTRATOR", "SUPPORT")

                        .antMatchers(HttpMethod.POST, "/api/antifraud/stolencard").hasRole("SUPPORT")
                        .antMatchers(HttpMethod.GET, "/api/antifraud/stolencard").hasRole("SUPPORT")
                        .antMatchers(HttpMethod.DELETE, "/api/antifraud/stolencard/*").hasRole("SUPPORT")
                        .antMatchers(HttpMethod.POST, "/api/antifraud/suspicious-ip").hasRole("SUPPORT")
                        .antMatchers(HttpMethod.GET, "/api/antifraud/suspicious-ip").hasRole("SUPPORT")
                        .antMatchers(HttpMethod.DELETE, "/api/antifraud/suspicious-ip/*").hasRole("SUPPORT")
                        .antMatchers(HttpMethod.PUT, "/api/antifraud/transaction").hasRole("SUPPORT")
                        .antMatchers(HttpMethod.GET, "/api/antifraud/history/**").hasRole("SUPPORT")

                        .antMatchers(HttpMethod.POST, "/api/antifraud/transaction/**").hasRole("MERCHANT")
                        .antMatchers("/actuator/shutdown").permitAll()      // needs to run test
                        .antMatchers("/h2-console/**").permitAll()

                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // no session
                )
                // other configurations
                .build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}