package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PartnerRepository partnerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") User user, Model model) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Email already registered.");
            return "register";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("PARTNER");

        // Partner details (set defaults or collect from form)
        Partner partner = new Partner();
        partner.setName(user.getName());
        partner.setMobile("");         // Or get from form
        partner.setSharePercent(0.0);  // Or get from form
        partner.setUser(user);         // Very important!

        user.setPartner(partner);

        userRepository.save(user);     // Only save User because of cascade

        return "redirect:/login?registered";
    }
}
