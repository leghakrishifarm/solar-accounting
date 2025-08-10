package com.legakrishi.solar.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class DebugWhoAmIController {

    @GetMapping("/debug/me")
    public Map<String, Object> me(Authentication auth) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (auth == null) {
            out.put("authenticated", false);
            return out;
        }
        out.put("authenticated", true);
        out.put("username", auth.getName());
        out.put("authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray());
        return out;
    }
}
