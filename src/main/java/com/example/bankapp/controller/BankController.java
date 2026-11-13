package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/")
    public String dashboard(@RequestParam(name = "keyword", required = false)
                            String keyword, Model model) {
        populateDashboard(model, keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String createCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateDashboard(model, null);
            return "bank";
        }

        try {
            customer.setId(null);
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully.");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateDashboard(model, null);
            return "bank";
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
        } catch (RuntimeException exception) {
            String reason = "The transfer could not be completed.";
            if (exception instanceof BankException) {
                reason = exception.getMessage();
            } else {
                log.error("Transfer failed", exception);
            }

            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    reason
            );
            redirectAttributes.addFlashAttribute("error", reason);
            return "redirect:/";
        }

        redirectAttributes.addFlashAttribute("success", "Transfer completed successfully.");
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable("accountNumber") String accountNumber,
                          Model model, RedirectAttributes redirectAttributes) {
        Customer customer;
        try {
            customer = bankService.getCustomer(accountNumber);
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/";
        }

        List<BankTransaction> transactions = bankService.getHistory(accountNumber);
        BigDecimal totalSent = bankService.getTotalSent(accountNumber);
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalSent", totalSent);
        return "history";
    }

    private void populateDashboard(Model model, String keyword) {
        List<Customer> allCustomers = bankService.getCustomers(null);

        model.addAttribute("allCustomers", allCustomers);
        model.addAttribute("customers",
                keyword == null || keyword.isBlank()
                        ? allCustomers
                        : bankService.getCustomers(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("totalCustomers", bankService.getTotalCustomers());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.countTransactions(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countTransactions(TransactionStatus.FAILED));

        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
    }

    @GetMapping("/large-transfers")
public String largeTransfers(
        @RequestParam(name = "min", defaultValue = "10000") BigDecimal min,
        Model model) {
    model.addAttribute("min", min);
    model.addAttribute("transfers", bankService.getLargeTransfers(min));
    return "large-transfers";
}

@GetMapping("/deposit")
public String depositPage(Model model) {
    model.addAttribute("customers", bankService.getCustomers(null));
    return "deposit";
}

@PostMapping("/deposit")
public String deposit(
        @RequestParam("accountNumber") String accountNumber,
        @RequestParam(name = "amount", required = false) BigDecimal amount,
        RedirectAttributes redirectAttributes) {
    try {
        bankService.deposit(accountNumber, amount);
        redirectAttributes.addFlashAttribute("success", "Deposit completed successfully.");
    } catch (BankException exception) {
        redirectAttributes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/deposit";
}

@GetMapping("/withdraw")
public String withdrawPage(Model model) {
    model.addAttribute("customers", bankService.getCustomers(null));
    return "withdraw";
}

@PostMapping("/withdraw")
public String withdraw(
        @RequestParam("accountNumber") String accountNumber,
        @RequestParam(name = "amount", required = false) BigDecimal amount,
        RedirectAttributes redirectAttributes) {
    try {
        bankService.withdraw(accountNumber, amount);
        redirectAttributes.addFlashAttribute("success", "Withdrawal completed successfully.");
    } catch (BankException exception) {
        redirectAttributes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/withdraw";
} 

}