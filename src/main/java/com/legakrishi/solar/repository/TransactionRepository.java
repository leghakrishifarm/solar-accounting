package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPartnerId(Long partnerId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type")
    Double sumAmountByType(@Param("type") Transaction.TransactionType type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.partner.id = :partnerId AND t.type = :type")
    Double getSumAmountByPartnerAndType(@Param("partnerId") Long partnerId, @Param("type") Transaction.TransactionType type);

    List<Transaction> findByType(Transaction.TransactionType type);

    // ✅ Added for Partner Dashboard: total outgoing (admin-level stat)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'OUTGOING'")
    double sumAllOutgoing();
}
