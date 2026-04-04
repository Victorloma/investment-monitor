package com.victorloma.investmentmonitor.portfolio;

import com.victorloma.investmentmonitor.portfolio.dto.CreatePortfolioEntryRequest;
import com.victorloma.investmentmonitor.portfolio.dto.PortfolioEntryResponse;
import com.victorloma.investmentmonitor.user.AppUser;
import com.victorloma.investmentmonitor.user.AppUserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private final UserPortfolioRepository userPortfolioRepository;
    private final CompanyRepository companyRepository;
    private final AppUserRepository appUserRepository;

    public PortfolioService(
            UserPortfolioRepository userPortfolioRepository,
            CompanyRepository companyRepository,
            AppUserRepository appUserRepository
    ) {
        this.userPortfolioRepository = userPortfolioRepository;
        this.companyRepository = companyRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<PortfolioEntryResponse> listPortfolio(UUID userId) {
        return userPortfolioRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PortfolioEntryResponse addPortfolioEntry(UUID userId, CreatePortfolioEntryRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedTicker = request.ticker().trim().toUpperCase(Locale.ROOT);
        String normalizedCompanyName = request.companyName() == null ? "" : request.companyName().trim();

        Company company = companyRepository.findByTickerIgnoreCase(normalizedTicker)
                .orElseGet(() -> createCompany(normalizedTicker, normalizedCompanyName));

        if (userPortfolioRepository.existsByUserIdAndCompanyId(userId, company.getId())) {
            throw new IllegalArgumentException("Company is already in your portfolio");
        }

        UserPortfolio portfolioEntry = new UserPortfolio();
        portfolioEntry.setUser(user);
        portfolioEntry.setCompany(company);
        portfolioEntry.setAlertThreshold(request.alertThreshold());
        portfolioEntry.setMonitored(request.monitored());

        return toResponse(userPortfolioRepository.save(portfolioEntry));
    }

    @Transactional
    public void removePortfolioEntry(UUID userId, UUID portfolioEntryId) {
        UserPortfolio portfolioEntry = userPortfolioRepository.findByIdAndUserId(portfolioEntryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio entry not found"));
        userPortfolioRepository.delete(portfolioEntry);
    }

    private Company createCompany(String ticker, String companyName) {
        if (companyName.isBlank()) {
            throw new IllegalArgumentException("Company name is required when adding a new ticker");
        }

        Company company = new Company();
        company.setTicker(ticker);
        company.setName(companyName);
        company.setActive(true);
        return companyRepository.save(company);
    }

    private PortfolioEntryResponse toResponse(UserPortfolio portfolioEntry) {
        return new PortfolioEntryResponse(
                portfolioEntry.getId(),
                portfolioEntry.getCompany().getTicker(),
                portfolioEntry.getCompany().getName(),
                portfolioEntry.getAlertThreshold(),
                portfolioEntry.isMonitored(),
                portfolioEntry.getAddedAt()
        );
    }
}
