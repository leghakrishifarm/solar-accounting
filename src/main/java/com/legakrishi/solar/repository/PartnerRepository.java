package com.legakrishi.solar.repository;
import com.legakrishi.solar.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.legakrishi.solar.model.Bill;

import java.util.Optional;
import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    Optional<Partner> findByUserId(Long userId);
    boolean existsByName(String name);
    Optional<Partner> findByName(String name);  // Use Optional to avoid NoSuchElementException

    @Query("SELECT p FROM Partner p WHERE p.user.email = :email")
    Optional<Partner> findByUserEmail(@Param("email") String email);

}
