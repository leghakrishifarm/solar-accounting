package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.Transaction;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.TransactionRepository;
import com.legakrishi.solar.repository.UserRepository;
import com.legakrishi.solar.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;

    @Autowired
    private TransactionService transactionService;

    public TransactionController(TransactionRepository transactionRepository,
                                 UserRepository userRepository,
                                 PartnerRepository partnerRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
    }

    // List all transactions (income, other income, outgoing, etc.)
    @GetMapping("")
    public String listTransactions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<Transaction> transactions;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            transactions = transactionRepository.findAll();
        } else {
            if (user.getPartner() != null) {
                transactions = transactionRepository.findByPartnerId(user.getPartner().getId());
            } else {
                transactions = List.of();
            }
        }
        model.addAttribute("transactions", transactions);
        return "admin/transactions/list";  // Create this Thymeleaf template
    }

    // --- Other Income ---

    // Show form to add Other Income
    @GetMapping("/other-income/add")
    public String showAddOtherIncomeForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("partners", partnerRepository.findAll());
        return "admin/transactions/other-income-form";  // Create this Thymeleaf template
    }

    // Save Other Income
    @PostMapping("/other-income/save")
    public String saveOtherIncome(@ModelAttribute @Valid Transaction transaction,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {

        if (result.hasErrors()) {
            model.addAttribute("partners", partnerRepository.findAll());
            return "admin/transactions/other-income-form";
        }

        // Set partner properly
        if (transaction.getPartner() != null && transaction.getPartner().getId() != null) {
            Long partnerId = transaction.getPartner().getId();
            if (partnerId > 0) {
                Partner partner = partnerRepository.findById(partnerId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid partner ID: " + partnerId));
                transaction.setPartner(partner);
            } else {
                transaction.setPartner(null);
            }
        } else {
            transaction.setPartner(null);
        }

        // Set who entered this transaction
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        transaction.setEnteredBy(user);

        if (transaction.getDateTime() == null) {
            transaction.setDateTime(LocalDateTime.now());
        }

        transaction.setType(Transaction.TransactionType.OTHER_INCOME);

        transactionRepository.save(transaction);

        redirectAttributes.addFlashAttribute("success", "Other income recorded successfully.");
        return "redirect:/admin/transactions";
    }

    // --- Outgoing Payments ---

    // Show form to add Outgoing Payment
    @GetMapping("/add/outgoing")
    public String showAddOutgoingForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("partners", partnerRepository.findAll());
        return "admin/transactions/add-outgoing";  // Create this Thymeleaf template
    }

    // Save Outgoing Payment
    @PostMapping("/add-outgoing/save")
    public String saveOutgoingPayment(@ModelAttribute @Valid Transaction transaction,
                                      BindingResult result,
                                      RedirectAttributes redirectAttributes,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partners", partnerRepository.findAll());
            return "admin/transactions/add-outgoing";
        }

        if (transaction.getPartner() != null && transaction.getPartner().getId() != null) {
            Long partnerId = transaction.getPartner().getId();
            if (partnerId > 0) {
                Partner partner = partnerRepository.findById(partnerId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid partner ID: " + partnerId));
                transaction.setPartner(partner);
            } else {
                transaction.setPartner(null);
            }
        } else {
            transaction.setPartner(null);
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        transaction.setEnteredBy(user);

        if (transaction.getDateTime() == null) {
            transaction.setDateTime(LocalDateTime.now());
        }

        transaction.setType(Transaction.TransactionType.OUTGOING);

        transactionRepository.save(transaction);

        redirectAttributes.addFlashAttribute("success", "Outgoing payment recorded successfully.");
        return "redirect:/admin/transactions/list/outgoing";
    }

    // List Outgoing Payments only
    @GetMapping("/list/outgoing")
    public String listOutgoingPayments(Model model) {
        List<Transaction> outgoings = transactionService.getAllOutgoingPayments();
        model.addAttribute("outgoings", outgoings);
        return "admin/transactions/list-outgoing";  // Create this Thymeleaf template
    }
}
