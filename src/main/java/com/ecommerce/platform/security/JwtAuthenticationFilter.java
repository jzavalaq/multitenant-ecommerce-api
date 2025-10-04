package com.ecommerce.platform.security;

import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.util.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter for validating JWT tokens on each request.
 * <p>
 * Extracts the JWT token from the Authorization header, validates it,
 * and sets the authentication in the security context.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);

            UserPrincipal principal = new UserPrincipal(userId, email, role, tenantId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user: {} with role: {} for tenant: {}", email, role, tenantId);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AppConstants.BEARER_PREFIX)) {
            return bearerToken.substring(AppConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Record representing the authenticated user principal.
     *
     * @param userId   the user ID
     * @param email    the user's email
     * @param role     the user's role
     * @param tenantId the tenant ID
     */
    public record UserPrincipal(Long userId, String email, String role, Long tenantId) {}
}
