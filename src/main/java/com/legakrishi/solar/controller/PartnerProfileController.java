package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping({"/partner", "/partners"})
public class PartnerProfileController {

    @Autowired
    private PartnerRepository partnerRepository;
    @Autowired
    private UserRepository userRepository;

    // PARTNER: View & update own profile
    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isPresent() && userOpt.get().getPartner() != null) {
            Partner partner = userOpt.get().getPartner();
            model.addAttribute("partner", partner);
            return "partners/profile";
        }
        model.addAttribute("error", "Partner profile not found!");
        return "partners/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute Partner formPartner,
                                Model model) {
        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isPresent() && userOpt.get().getPartner() != null) {
            Partner partner = userOpt.get().getPartner();
            partner.setMobile(formPartner.getMobile());
            partner.setSharePercent(formPartner.getSharePercent());
            partner.setItrStatus(formPartner.getItrStatus());
            partner.setGstStatus(formPartner.getGstStatus());
            partnerRepository.save(partner);
            model.addAttribute("partner", partner);
            model.addAttribute("success", "Profile updated!");
            return "partners/profile";
        }
        model.addAttribute("error", "Partner profile not found!");
        return "partners/profile";
    }

    // ADMIN: View all partners
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin/list")
    public String listPartners(Model model) {
        List<Partner> partners = partnerRepository.findAll();
        model.addAttribute("partners", partners);
        return "admin/partners-list";
    }

    // ADMIN: Edit any partner
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin/edit/{id}")
    public String editPartner(@PathVariable Long id, Model model) {
        Optional<Partner> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isPresent()) {
            model.addAttribute("partner", partnerOpt.get());
            return "admin/partner-edit";
        }
        model.addAttribute("error", "Partner not found");
        return "redirect:/partner/admin/list";
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/admin/edit/{id}")
    public String updatePartner(@PathVariable Long id,
                                @ModelAttribute Partner formPartner,
                                Model model) {
        Optional<Partner> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isPresent()) {
            Partner partner = partnerOpt.get();
            partner.setMobile(formPartner.getMobile());
            partner.setSharePercent(formPartner.getSharePercent());
            partner.setItrStatus(formPartner.getItrStatus());
            partner.setGstStatus(formPartner.getGstStatus());
            partnerRepository.save(partner);
            model.addAttribute("success", "Partner updated!");
            return "redirect:/partner/admin/list";
        }
        model.addAttribute("error", "Partner not found");
        return "redirect:/partner/admin/list";
    }
}
