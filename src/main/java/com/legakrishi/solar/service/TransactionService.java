package com.legakrishi.solar.service;

import com.legakrishi.solar.model.Transaction;
import com.legakrishi.solar.model.Transaction.TransactionType;
import com.legakrishi.solar.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    // Save outgoing payment with type and timestamp set
    public Transaction saveOutgoingPayment(Transaction transaction) {
        transaction.setType(TransactionType.OUTGOING);
        if (transaction.getDateTime() == null) {
            transaction.setDateTime(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    // Sum of all outgoing amounts
    public Double getTotalOutgoingAmount() {
        Double total = transactionRepository.sumAmountByType(TransactionType.OUTGOING);
        return total != null ? total : 0.0;
    }

    // Get all outgoing transactions
    public List<Transaction> getAllOutgoingPayments() {
        return transactionRepository.findByType(TransactionType.OUTGOING);
    }
}
