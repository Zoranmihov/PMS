package PMS.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import PMS.user.Util.JwtUtil;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil) {
                return new JwtAuthFilter(jwtUtil);
        }

        // Routes

        private static final String[] PUBLIC = {
                        "/api/v1/users/register",
                        "/api/v1/users/login",
                        "/api/v1/users/refresh",
                        "/api/v1/users/activate",
                        "/api/v1/users/user/delete"
        };

        private static final String[] USER = {
                        "/api/v1/users/test",
                        "/api/v1/users/user/requestdelete",
                        "/api/v1/users/user/update"
                        
        };

        private static final String[] ADMIN = {
                "/api/v1/admin/user/{id}/roles",
                "/api/v1/admin/user/delete",
                "/api/v1/admin/user/update"
        };

        @Bean
        RoleHierarchy roleHierarchy() {
                return RoleHierarchyImpl.fromHierarchy("""
                                    ROLE_ADMIN > ROLE_USER
                                """);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter)
                        throws Exception {
                return http
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC).permitAll()
                                                .requestMatchers(USER).hasRole("USER")
                                                .requestMatchers(ADMIN).hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .csrf(csrf -> csrf.disable())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }
}
