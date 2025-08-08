package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.Transaction;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.TransactionRepository;
import com.legakrishi.solar.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class PartnerTransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/partners/transactions")
    @PreAuthorize("hasRole('PARTNER')")
    public String viewPartnerTransactions(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Partner partner = user.getPartner();

        List<Transaction> transactions = transactionRepository.findByPartnerId(partner.getId());
        model.addAttribute("transactions", transactions);
        model.addAttribute("page", "transactions"); // 🔹 For sidebar active

        return "partners/partner-transactions";
    }
}
