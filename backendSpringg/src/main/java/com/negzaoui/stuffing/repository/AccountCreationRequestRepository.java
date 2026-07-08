package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.AccountCreationRequest;
import com.negzaoui.stuffing.entity.AccountRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountCreationRequestRepository extends JpaRepository<AccountCreationRequest, Long> {

    boolean existsByEmailAndStatus(String email, AccountRequestStatus status);

    Page<AccountCreationRequest> findAllByStatusOrderByCreatedAtDesc(AccountRequestStatus status, Pageable pageable);

    Page<AccountCreationRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(AccountRequestStatus status);
}
