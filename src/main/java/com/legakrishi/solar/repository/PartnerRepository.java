package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    Optional<Partner> findByMobile(String mobile);

    Optional<Partner> findByNameIgnoreCase(String name);

    // Kept for compatibility if some controllers still call it
    Optional<Partner> findByName(String name);

    // ✅ Resolve Partner via linked User.email  (ADJUST field names if needed)
    @Query("""
        select p from Partner p
        join p.user u
        where lower(u.email) = lower(:email)
    """)
    Optional<Partner> findByUserEmail(@Param("email") String email);

    // IMPORTANT:
    // - DO NOT keep any findByEmailIgnoreCase on Partner (Partner has no 'email' field)
    // - REMOVE findByUserUsernameIgnoreCase (your User has no 'username' field)
    // If your User field is 'emailId' instead of 'email', replace u.email with u.emailId above.
}
