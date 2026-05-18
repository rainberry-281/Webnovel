package com.bn.berrynovel.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CustomSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private UserService userService;
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    protected String determineTargetUrl(final Authentication authentication) {

        Map<String, String> roleTargetUrlMap = new HashMap<>();
        roleTargetUrlMap.put("ROLE_USER", "/");
        roleTargetUrlMap.put("ROLE_ADMIN", "/admin");
        /* Role-based routing */

        final Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        /* Get the user's authorities after a successful login. */
        for (final GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();

            if (roleTargetUrlMap.containsKey(authorityName)) {
                return roleTargetUrlMap.get(authorityName);
            }
        }

        throw new IllegalStateException();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        /* Clear the previous login error after a successful login. */

        String username = authentication.getName(); // Issue starts here.
        User user = this.userService.getUserByUsername(username);

        if (user != null) {
            session.setAttribute("username", user.getUsername());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("id", user.getId());
            session.setAttribute("phoneNumber", user.getPhoneNumber());
            session.setAttribute("email", user.getEmail());
            // int sum = user.getCart() == null ? 0 : user.getCart().getSum();
            session.setAttribute("avatar", user.getImage());
            // session.setAttribute("sum", sum);

            /*
             * Front-end session access example:
             * <p>Hello, ${sessionScope.fullName}</p>
             * <p>Email: ${sessionScope.email}</p>
             */
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(authentication);

        if (response.isCommitted()) {
            return;
        }

        redirectStrategy.sendRedirect(request, response, targetUrl); /* Redirect page */
        clearAuthenticationAttributes(request, authentication);
    }

}
