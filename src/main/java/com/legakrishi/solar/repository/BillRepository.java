package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.Bill;
import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByStatus(String status);

    List<Bill> findByPartnerId(Long partnerId);

    boolean existsByJmrReport(JmrReport jmrReport);

    Optional<Bill> findTopByOrderByReadingDateDesc();

    List<Bill> findAllByPartner(Partner partner);

    List<Bill> findAllByPartnerAndStatus(Partner partner, String status);

    List<Bill> findAllByPartnerAndJmrReportIsNotNull(Partner partner);

    List<Bill> findAllByJmrReportIsNotNull();

    @Query("SELECT SUM(b.amount) FROM Bill b WHERE b.status = :status")
    Double sumAmountByStatus(@Param("status") String status);

    @Query("SELECT SUM(b.actualAmountReceived) FROM Bill b WHERE b.status = :status")
    Double sumActualAmountReceivedByStatus(String status);

    @Query("SELECT SUM(b.governmentDeductions) FROM Bill b WHERE b.status = :status")
    Double sumGovernmentDeductionsByStatus(String status);

    // ✅ NEW: Count all bills by partner
    int countByPartner(Partner partner);

    // ✅ NEW: Sum total paidAmount by partner
    @Query("SELECT COALESCE(SUM(b.actualAmountReceived), 0) FROM Bill b WHERE b.partner = :partner")
    double sumPaidAmountByPartner(@Param("partner") Partner partner);

    // ✅ NEW: Get last bill status for a partner
    @Query("SELECT b.status FROM Bill b WHERE b.partner = :partner ORDER BY b.id DESC")
    List<String> findLastBillStatusListByPartner(@Param("partner") Partner partner);

    // ✅ Convenience method (used in controller to get only first if available)
    default String findLastBillStatusByPartner(Partner partner) {
        List<String> statuses = findLastBillStatusListByPartner(partner);
        return statuses.isEmpty() ? "-" : statuses.get(0);
    }

    // ✅ NEW: Admin-level - sum of all paid amounts
    @Query("SELECT SUM(b.actualAmountReceived) FROM Bill b")
    Double sumAllPaidAmount(); // ✅ Correct field name based on your Bill entity


    // ✅ NEW: Admin-level - sum of all government deductions
    @Query("SELECT COALESCE(SUM(b.governmentDeductions), 0) FROM Bill b")
    double sumAllGovtDeductions();
}
