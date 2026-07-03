package com.easyimmo.billing.repository;

import com.easyimmo.billing.model.SaaSInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaaSInvoiceRepository extends JpaRepository<SaaSInvoice, UUID> {
    Page<SaaSInvoice> findByAgencyId(UUID agencyId, Pageable pageable);
    
    List<SaaSInvoice> findByAgencyIdAndStatus(UUID agencyId, SaaSInvoice.InvoiceStatus status);

    Page<SaaSInvoice> findByStatus(SaaSInvoice.InvoiceStatus status, Pageable pageable);
}
