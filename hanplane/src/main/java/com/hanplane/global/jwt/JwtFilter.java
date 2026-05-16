package com.hanplane.global.jwt;

import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info("url검증 : " + request.getRequestURL());

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);

        log.info("jwt filter에서 subString 7 이후 token 디버깅 : " + token);
        boolean isValidated = false;
        try {
            isValidated = jwtProvider.validateToken(token);
        } catch (BusinessException e) {
            ErrorCode error = e.getErrorCode();
            response.setStatus(error.getStatus());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + error.getCode() + ",\"message\":\"" + error.getMessage() + "\",\"data\":null}");
            return;
        }
        log.info(isValidated + " 검증 여부 isValidated");
        if (!isValidated) {
            filterChain.doFilter(request, response);
            return;
        }

        UserPrincipal principal = jwtProvider.getPrincipal(token);

        log.info(principal.toString() + "프린씨플 toString");

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));

        log.info(authorities.toString() + " authorities디버깅 toString");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        log.info(authentication.toString() + " authentication디버깅 toString");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);

    }

}
