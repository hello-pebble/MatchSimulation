package com.pebble.adminhub.user.security;

import com.pebble.adminhub.user.domain.User;
import com.pebble.adminhub.user.domain.UserStatus;
import com.pebble.adminhub.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * X-AUTH-TOKEN 헤더의 JWT를 검증해 SecurityContext에 인증을 주입한다.
 * 정지(SUSPENDED)된 계정은 유효한 토큰이라도 인증을 부여하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-AUTH-TOKEN";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    /**
     * Long Polling(DeferredResult)의 ASYNC 디스패치에서도 재인증한다 —
     * 기본값(true)이면 비동기 응답 재개 시 SecurityContext가 비어 401이 된다.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        jwtProvider.parseUserId(request.getHeader(HEADER))
                .flatMap(userRepository::findById)
                .filter(user -> user.getStatus() != UserStatus.SUSPENDED)
                .ifPresent(this::authenticate);
        chain.doFilter(request, response);
    }

    private void authenticate(User user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
