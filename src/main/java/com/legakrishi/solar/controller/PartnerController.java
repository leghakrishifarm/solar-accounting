package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/partners")
public class PartnerController {

    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;

    public PartnerController(PartnerRepository partnerRepository, UserRepository userRepository) {
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("")
    public String listPartners(Model model) {
        List<Partner> partners = partnerRepository.findAll();
        model.addAttribute("partners", partners);
        return "admin/partners/list";
    }

    @GetMapping("/add")
    public String newPartnerForm(Model model) {
        model.addAttribute("partner", new Partner());
        model.addAttribute("users", userRepository.findByRoleAndPartnerIsNull("PARTNER"));
        return "admin/partners/form";
    }

    @GetMapping("/edit/{id}")
    public String editPartner(@PathVariable Long id, Model model) {
        Optional<Partner> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isEmpty()) {
            model.addAttribute("error", "Partner with id " + id + " not found.");
            return "redirect:/admin/partners";
        }
        Partner partner = partnerOpt.get();
        model.addAttribute("partner", partner);

        List<User> users = userRepository.findByRoleAndPartnerIsNull("PARTNER");
        // Add currently linked user if missing from the list
        if (partner.getUser() != null && !users.contains(partner.getUser())) {
            users.add(partner.getUser());
        }
        model.addAttribute("users", users);
        return "admin/partners/form";
    }

    @PostMapping("/save")
    @Transactional
    public String savePartner(@ModelAttribute Partner partner, Model model) {
        // Basic validation
        if (partner.getName() == null || partner.getName().isBlank()) {
            model.addAttribute("error", "Name is required!");
            model.addAttribute("users", userRepository.findByRoleAndPartnerIsNull("PARTNER"));
            return "admin/partners/form";
        }
        if (partner.getMobile() == null || partner.getMobile().isBlank()) {
            model.addAttribute("error", "Mobile is required!");
            model.addAttribute("users", userRepository.findByRoleAndPartnerIsNull("PARTNER"));
            return "admin/partners/form";
        }

        // Check duplicate partner name (case sensitive)
        Optional<Partner> existingByNameOpt = partnerRepository.findByName(partner.getName());
        if (existingByNameOpt.isPresent()) {
            Partner existingByName = existingByNameOpt.get();
            if (partner.getId() == null || !existingByName.getId().equals(partner.getId())) {
                model.addAttribute("error", "Partner name already exists!");
                model.addAttribute("users", userRepository.findByRoleAndPartnerIsNull("PARTNER"));
                return "admin/partners/form";
            }
        }

        // Unlink old user if user changed
        if (partner.getId() != null) {
            Partner oldPartner = partnerRepository.findById(partner.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid partner Id"));
            User oldUser = oldPartner.getUser();
            if (oldUser != null && (partner.getUser() == null || !oldUser.getId().equals(partner.getUser().getId()))) {
                oldUser.setPartner(null);
                userRepository.save(oldUser);

                oldPartner.setUser(null);
                partnerRepository.save(oldPartner);
            }
        }

        // Set full user entity if user selected
        if (partner.getUser() != null && partner.getUser().getId() != null) {
            User user = userRepository.findById(partner.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid user Id"));
            partner.setUser(user);
        } else {
            partner.setUser(null);
        }

        // Save partner entity
        partnerRepository.save(partner);

        // Link partner to user
        if (partner.getUser() != null) {
            User user = partner.getUser();
            user.setPartner(partner);
            userRepository.save(user);
        }

        return "redirect:/admin/partners";
    }

    @PostMapping("/delete/{id}")
    @Transactional
    public String deletePartner(@PathVariable Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid partner Id: " + id));

        // Unlink user.partner
        User user = partner.getUser();
        if (user != null) {
            user.setPartner(null);
            userRepository.save(user);
        }

        partnerRepository.delete(partner);
        return "redirect:/admin/partners";
    }
}
