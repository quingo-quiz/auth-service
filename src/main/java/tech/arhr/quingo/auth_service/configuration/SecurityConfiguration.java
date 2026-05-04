package tech.arhr.quingo.auth_service.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import tech.arhr.quingo.auth_service.api.rest.filters.JwtAuthenticationFilter;
import tech.arhr.quingo.auth_service.api.rest.filters.RequestsLogFilter;
import tech.arhr.quingo.auth_service.api.security.AnonymousAuthenticationDetailsSource;
import tech.arhr.quingo.auth_service.api.security.CustomAnonymousAuthenticationFilter;
import tech.arhr.quingo.auth_service.api.security.CustomAuthenticationDetailsSource;
import tech.arhr.quingo.auth_service.api.security.handlers.CustomAccessDeniedHandler;
import tech.arhr.quingo.auth_service.api.security.handlers.CustomAuthenticationEntryPoint;
import tech.arhr.quingo.auth_service.services.oauth2.CustomOAuth2UserService;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2FailureHandler;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2SuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestsLogFilter requestsLogFilter;

    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomAuthenticationDetailsSource customAuthenticationDetailsSource;
    private final CustomAnonymousAuthenticationFilter customAnonymousAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // disable basic endpoints and logic
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(requests -> requests
                                .requestMatchers(
                                        "/register",
                                        "/auth",
                                        "/logout",
                                        "/logout/all",
                                        "/refresh",
                                        "/error",
                                        "/oauth2/authorization/**",
                                        "/mfa/otp/verify"
                                ).permitAll()

                                .anyRequest().authenticated()
                        //.anyRequest().permitAll()
                )


                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                .addFilterBefore(customAnonymousAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestsLogFilter, UsernamePasswordAuthenticationFilter.class)

                .oauth2Login(oauth2 -> oauth2
                        .authenticationDetailsSource(customAuthenticationDetailsSource)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                .build();
    }
}
