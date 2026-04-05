package com.victorloma.investmentmonitor.briefing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IrBriefingRepository extends JpaRepository<IrBriefing, UUID> {
  Optional<IrBriefing> findTopByPortfolioEntryIdOrderByFetchedAtDesc(UUID portfolioEntryId);
}
