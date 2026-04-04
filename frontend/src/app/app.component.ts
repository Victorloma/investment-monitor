import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { PortfolioEntry, PortfolioService } from './portfolio.service';

type ActivityItem = {
  company: string;
  event: string;
  timestamp: string;
};

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
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

  readonly refreshProgress = 68;

  protected isLoading = false;
  protected authMessage = '';
  protected authError = '';
  protected currentUser: CurrentUser | null = null;
  protected authResponse: AuthResponse | null = null;
  protected portfolioEntries: PortfolioEntry[] = [];

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
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
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
    const trimmedAlertThreshold = rawValue.alertThreshold.trim();

    this.portfolioService
      .create({
        ticker: rawValue.ticker.trim().toUpperCase(),
        companyName: trimmedCompanyName,
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
            alertThreshold: '',
          });
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        },
      });
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
    fieldName: 'ticker' | 'companyName' | 'alertThreshold',
  ): boolean {
    const field = this.portfolioForm.controls[fieldName];
    return Boolean(field.invalid && (field.dirty || field.touched));
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
