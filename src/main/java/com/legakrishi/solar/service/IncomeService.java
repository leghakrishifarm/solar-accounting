package com.legakrishi.solar.service;

import com.legakrishi.solar.model.Bill;
import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IncomeService {

    @Autowired
    private PartnerRepository partnerRepository;

    public void updateIncomeAfterPayment(Bill bill) {
        Partner partner = bill.getPartner();
        if (partner == null) return;

        double received = bill.getActualAmountReceived() != null ? bill.getActualAmountReceived() : 0.0;
        double deductions = bill.getGovernmentDeductions() != null ? bill.getGovernmentDeductions() : 0.0;

        partner.setTotalIncomeReceived(
                (partner.getTotalIncomeReceived() != null ? partner.getTotalIncomeReceived() : 0.0) + received
        );
        partner.setTotalGovernmentDeductions(
                (partner.getTotalGovernmentDeductions() != null ? partner.getTotalGovernmentDeductions() : 0.0) + deductions
        );

        partnerRepository.save(partner);
    }
}

