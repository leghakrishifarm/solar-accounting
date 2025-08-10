package com.legakrishi.solar.service;

import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.PartnerSiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PartnerAccessService {

    private static final Logger log = LoggerFactory.getLogger(PartnerAccessService.class);

    private final PartnerRepository partnerRepo;
    private final PartnerSiteRepository partnerSiteRepo;

    /** When true, deny access if partner cannot be resolved or doesn't own the site. When false, allow (dev). */
    private final boolean strict;

    public PartnerAccessService(PartnerRepository partnerRepo,
                                PartnerSiteRepository partnerSiteRepo,
                                @Value("${lkf.security.partnerStrict:false}") boolean strict) {
        this.partnerRepo = partnerRepo;
        this.partnerSiteRepo = partnerSiteRepo;
        this.strict = strict;
    }

    /** Throws AccessDeniedException if logged-in partner doesn't own siteId (when strict=true). */
    public void assertHasSite(Long siteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthenticated");
        }

        final String username = auth.getName().trim(); // e.g., "partnerA@solar.com"
        log.debug("PartnerAccessService: resolving partner for username='{}'", username);

        Optional<Partner> partnerOpt = Optional.empty();

        // 1) mobile (exact)
        try { partnerOpt = partnerRepo.findByMobile(username); }
        catch (Throwable t) { log.debug("findByMobile skipped/failed: {}", t.toString()); }

        // 2) name (case-insensitive)
        if (partnerOpt.isEmpty()) {
            try { partnerOpt = partnerRepo.findByNameIgnoreCase(username); }
            catch (Throwable t) { log.debug("findByNameIgnoreCase skipped/failed: {}", t.toString()); }
        }

        // 3) linked user.email (only attempt if username looks like an email)
        if (partnerOpt.isEmpty() && username.contains("@")) {
            try { partnerOpt = partnerRepo.findByUserEmail(username); }
            catch (Throwable t) { log.debug("findByUserEmail skipped/failed: {}", t.toString()); }
        }

        if (partnerOpt.isEmpty()) {
            String msg = "Partner not found for username '" + username + "'";
            if (strict) throw new org.springframework.security.access.AccessDeniedException(msg);
            log.warn("PartnerAccessService: {}. Proceeding (strict=false). Configure proper lookup when ready.", msg);
            return; // allow in non-strict mode so dev/testing can continue
        }

        var partner = partnerOpt.get();
        boolean ok;
        try {
            ok = partnerSiteRepo.existsByPartnerIdAndSiteId(partner.getId(), siteId);
        } catch (Throwable t) {
            log.warn("PartnerAccessService: partnerSiteRepo check failed: {}", t.toString());
            ok = false;
        }

        if (!ok) {
            String msg = "No access to site " + siteId;
            if (strict) throw new org.springframework.security.access.AccessDeniedException(msg);
            log.warn("PartnerAccessService: {} (partnerId={}). Proceeding (strict=false). Backfill partner_site mapping.",
                    msg, partner.getId());
        }
    }
}
