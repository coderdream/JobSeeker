package com.wh.jobsbackend.application.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Long requireUserId() {
        return requirePrincipal().getId();
    }

    public String requireUsername() {
        return requirePrincipal().getUsername();
    }

    public String requireRole() {
        return requirePrincipal().getRole();
    }

    public AppUserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal)) {
            throw new IllegalStateException("Unsupported principal type: " + principal);
        }
        return appUserPrincipal;
    }
}
