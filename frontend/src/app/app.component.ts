import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { finalize } from 'rxjs';
import { AuthResponse, AuthService, CurrentUser } from './auth.service';
import { BriefingContent, BriefingService, IrBriefingResponse, UserSettingsResponse } from './briefing.service';
import { CrawlPreviewResponse, PortfolioEntry, PortfolioService } from './portfolio.service';

type ActivityItem = {
  company: string;
  event: string;
  timestamp: string;
};

const HTTP_URL_PATTERN = /^https?:\/\/\S+$/i;

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressBarModule,
    MatToolbarModule,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly portfolioService = inject(PortfolioService);
  private readonly briefingService = inject(BriefingService);

  readonly refreshProgress = 68;

  protected isLoading = false;
  protected authMessage = '';
  protected authError = '';
  protected currentUser: CurrentUser | null = null;
  protected authResponse: AuthResponse | null = null;
  protected portfolioEntries: PortfolioEntry[] = [];
  protected crawlPreviews = new Map<string, CrawlPreviewResponse>();
  protected crawlingIds = new Set<string>();

  protected userSettings: UserSettingsResponse | null = null;
  protected briefings = new Map<string, BriefingContent>();
  protected fetchingBriefingIds = new Set<string>();
  protected isRefreshingAll = false;
  protected showSettingsPanel = false;
  protected apiKeyInput = '';
  protected providerUrlInput = 'https://generativelanguage.googleapis.com/v1beta/openai/';
  protected modelIdInput = 'gemini-2.0-flash-lite';
  protected isSavingApiKey = false;

  protected readonly loginForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected readonly registerForm = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(100)],
    ],
  });

  protected readonly portfolioForm = this.formBuilder.nonNullable.group({
    ticker: ['', [Validators.required, Validators.maxLength(10)]],
    companyName: ['', [Validators.maxLength(255)]],
    irUrl: ['', [Validators.maxLength(500), Validators.pattern(HTTP_URL_PATTERN)]],
    alertThreshold: [''],
  });

  readonly recentActivity: ActivityItem[] = [
    {
      company: 'Microsoft',
      event: 'Quarterly earnings release detected',
      timestamp: '2 min ago',
    },
    {
      company: 'Apple',
      event: 'Investor relations page refresh started',
      timestamp: '8 min ago',
    },
    {
      company: 'NVIDIA',
      event: 'New SEC filing summarized',
      timestamp: '14 min ago',
    },
  ];

  ngOnInit(): void {
    if (this.authService.hasToken()) {
      this.loadCurrentUser(true);
    }
  }

  protected submitLogin(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.authError = '';
    this.authMessage = '';

    this.authService
      .login(this.loginForm.getRawValue())
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (response) => {
          this.authResponse = response;
          this.authMessage = `Signed in as ${response.email}`;
          this.loginForm.reset({ email: '', password: '' });
          this.loadCurrentUser(true);
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
      });
  }

  protected submitRegister(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.authError = '';
    this.authMessage = '';

    this.authService
      .register(this.registerForm.getRawValue())
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (response) => {
          this.authResponse = response;
          this.authMessage = `Registered ${response.email} successfully`;
          this.registerForm.reset({
            firstName: '',
            lastName: '',
            email: '',
            password: '',
          });
          this.loadCurrentUser(true);
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
      });
  }

  protected loadCurrentUser(loadPortfolio = false): void {
    this.isLoading = true;
    this.authError = '';

    this.authService
      .me()
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          if (loadPortfolio) {
            this.loadPortfolio();
          }
        },
        error: (error) => {
          this.currentUser = null;
          this.authResponse = null;
          this.portfolioEntries = [];
          this.authService.logout();
          this.authError = this.extractErrorMessage(error);
        },
      });
  }

  protected loadPortfolio(): void {
    this.isLoading = true;
    this.authError = '';

    this.portfolioService
      .list()
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (entries) => {
          this.portfolioEntries = entries;
          this.loadSettings();
          entries.forEach((e) => this.loadPersistedBriefing(e.id));
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
      });
  }

  protected loadSettings(): void {
    this.briefingService.getSettings().subscribe({
      next: (s) => {
        this.userSettings = s;
        this.providerUrlInput = s.providerUrl;
        this.modelIdInput = s.modelId;
      },
      error: () => {},
    });
  }

  private loadPersistedBriefing(portfolioEntryId: string): void {
    this.briefingService.getLatestBriefing(portfolioEntryId).subscribe({
      next: (response) => {
        if (response) {
          this.storeBriefing(response);
        }
      },
      error: () => {},
    });
  }

  protected fetchBriefing(portfolioEntryId: string): void {
    this.fetchingBriefingIds.add(portfolioEntryId);
    this.authError = '';

    this.briefingService
      .fetchBriefing(portfolioEntryId)
      .pipe(finalize(() => this.fetchingBriefingIds.delete(portfolioEntryId)))
      .subscribe({
        next: (response) => this.storeBriefing(response),
        error: (error) => (this.authError = this.extractErrorMessage(error)),
      });
  }

  protected refreshAllBriefings(): void {
    this.isRefreshingAll = true;
    this.authError = '';

    this.briefingService
      .refreshAll()
      .pipe(finalize(() => (this.isRefreshingAll = false)))
      .subscribe({
        next: (responses) => responses.forEach((r) => this.storeBriefing(r)),
        error: (error) => (this.authError = this.extractErrorMessage(error)),
      });
  }

  private storeBriefing(response: IrBriefingResponse): void {
    const content = this.briefingService.parseBriefingJson(response.briefingJson);
    if (content) {
      this.briefings.set(response.portfolioEntryId, content);
    }
  }

  protected saveApiKey(): void {
    if (!this.apiKeyInput.trim()) return;
    this.isSavingApiKey = true;
    this.authError = '';

    this.briefingService
      .saveSettings({
        apiKey: this.apiKeyInput.trim(),
        providerUrl: this.providerUrlInput.trim(),
        modelId: this.modelIdInput.trim(),
      })
      .pipe(finalize(() => (this.isSavingApiKey = false)))
      .subscribe({
        next: () => {
          this.userSettings = {
            hasApiKey: true,
            providerUrl: this.providerUrlInput.trim(),
            modelId: this.modelIdInput.trim(),
          };
          this.apiKeyInput = '';
          this.showSettingsPanel = false;
          this.authMessage = 'Settings saved';
        },
        error: (error: { error?: unknown; message?: string }) =>
          (this.authError = this.extractErrorMessage(error)),
      });
  }

  protected submitPortfolioEntry(): void {
    if (this.portfolioForm.invalid) {
      this.portfolioForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.authError = '';
    this.authMessage = '';

    const rawValue = this.portfolioForm.getRawValue();
    const trimmedCompanyName = rawValue.companyName.trim();
    const trimmedIrUrl = rawValue.irUrl.trim();
    const trimmedAlertThreshold = rawValue.alertThreshold.trim();

    this.portfolioService
      .create({
        ticker: rawValue.ticker.trim().toUpperCase(),
        companyName: trimmedCompanyName,
        irUrl: trimmedIrUrl || null,
        alertThreshold: trimmedAlertThreshold
          ? Number(trimmedAlertThreshold)
          : null,
        monitored: true,
      })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (entry) => {
          this.portfolioEntries = [entry, ...this.portfolioEntries];
          this.authMessage = `${entry.ticker} added to your portfolio`;
          this.portfolioForm.reset({
            ticker: '',
            companyName: '',
            irUrl: '',
            alertThreshold: '',
          });
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
      });
  }

  protected loadCrawlPreview(portfolioEntryId: string): void {
    this.crawlingIds.add(portfolioEntryId);
    this.authError = '';

    this.portfolioService
      .crawlPreview(portfolioEntryId)
      .pipe(finalize(() => this.crawlingIds.delete(portfolioEntryId)))
      .subscribe({
        next: (preview) => {
          this.crawlPreviews.set(portfolioEntryId, preview);
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
      });
  }

  protected dismissCrawlPreview(portfolioEntryId: string): void {
    this.crawlPreviews.delete(portfolioEntryId);
  }

  protected removePortfolioEntry(portfolioEntryId: string): void {
    this.isLoading = true;
    this.authError = '';
    this.authMessage = '';

    this.portfolioService
      .remove(portfolioEntryId)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: () => {
          this.portfolioEntries = this.portfolioEntries.filter(
            (entry) => entry.id !== portfolioEntryId,
          );
          this.authMessage = 'Portfolio entry removed';
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
      });
  }

  protected logout(): void {
    this.authService.logout();
    this.currentUser = null;
    this.authResponse = null;
    this.portfolioEntries = [];
    this.briefings.clear();
    this.userSettings = null;
    this.showSettingsPanel = false;
    this.authMessage = 'Signed out';
    this.authError = '';
  }

  protected hasLoginFieldError(fieldName: 'email' | 'password'): boolean {
    const field = this.loginForm.controls[fieldName];
    return Boolean(field.invalid && (field.dirty || field.touched));
  }

  protected hasRegisterFieldError(
    fieldName: 'firstName' | 'lastName' | 'email' | 'password',
  ): boolean {
    const field = this.registerForm.controls[fieldName];
    return Boolean(field && field.invalid && (field.dirty || field.touched));
  }

  protected hasPortfolioFieldError(
    fieldName: 'ticker' | 'companyName' | 'irUrl' | 'alertThreshold',
  ): boolean {
    const field = this.portfolioForm.controls[fieldName];
    return Boolean(field.invalid && (field.dirty || field.touched));
  }

  protected hasPortfolioFieldSpecificError(
    fieldName: 'ticker' | 'companyName' | 'irUrl' | 'alertThreshold',
    errorName: string,
  ): boolean {
    const field = this.portfolioForm.controls[fieldName];
    return Boolean(field.hasError(errorName) && (field.dirty || field.touched));
  }

  private extractErrorMessage(error: {
    error?: unknown;
    message?: string;
  }): string {
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }

    return error.message ?? 'Something went wrong while calling the API';
  }

  trackByPortfolioEntry(_: number, item: PortfolioEntry): string {
    return item.id;
  }

  trackByActivity(_: number, item: ActivityItem): string {
    return `${item.company}-${item.timestamp}`;
  }
}
