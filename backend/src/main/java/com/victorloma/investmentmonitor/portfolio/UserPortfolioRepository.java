package com.victorloma.investmentmonitor.portfolio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, UUID> {

  List<UserPortfolio> findByUserIdOrderByAddedAtDesc(UUID userId);

  Optional<UserPortfolio> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
}
