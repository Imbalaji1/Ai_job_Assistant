package com.jobtracker.security;

import com.jobtracker.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Requires a non-simple request header for cookie-setting auth operations.
 * Browsers cannot add this header cross-origin unless the server explicitly
 * permits a CORS preflight; this API intentionally does not enable CORS.
 */
public final class AuthRequestProtectionFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Requested-With";
    public static final String HEADER_VALUE = "AIJobAssistant";

    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final JsonMapper jsonMapper;

    public AuthRequestProtectionFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isProtectedAuthRequest(request) && !hasApplicationRequestHeader(request)) {
            writeForbidden(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isProtectedAuthRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth/")
                && PROTECTED_METHODS.contains(request.getMethod().toUpperCase());
    }

    private boolean hasApplicationRequestHeader(HttpServletRequest request) {
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        return HEADER_VALUE.equals(request.getHeader(HEADER_NAME))
                && !"cross-site".equalsIgnoreCase(fetchSite);
    }

    private void writeForbidden(HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(), HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(),
                "The authentication request was rejected.", request.getRequestURI()));
    }
}
