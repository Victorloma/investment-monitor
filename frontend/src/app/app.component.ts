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

type WatchlistItem = {
  ticker: string;
  company: string;
  status: 'Monitoring' | 'Refreshing' | 'Paused';
  updates: number;
};

type ActivityItem = {
  company: string;
  event: string;
  timestamp: string;
};

@Component({
  selector: 'app-root',
  standalone: true,
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
    MatToolbarModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly portfolioCount = 12;
  readonly unreadUpdates = 8;
  readonly refreshProgress = 68;

  protected isLoading = false;
  protected authMessage = '';
  protected authError = '';
  protected currentUser: CurrentUser | null = null;
  protected authResponse: AuthResponse | null = null;

  protected readonly loginForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  protected readonly registerForm = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]]
  });

  readonly watchlist: WatchlistItem[] = [
    { ticker: 'MSFT', company: 'Microsoft', status: 'Monitoring', updates: 2 },
    { ticker: 'AAPL', company: 'Apple', status: 'Refreshing', updates: 1 },
    { ticker: 'NVDA', company: 'NVIDIA', status: 'Monitoring', updates: 3 },
    { ticker: 'ASML', company: 'ASML', status: 'Paused', updates: 0 }
  ];

  readonly recentActivity: ActivityItem[] = [
    { company: 'Microsoft', event: 'Quarterly earnings release detected', timestamp: '2 min ago' },
    { company: 'Apple', event: 'Investor relations page refresh started', timestamp: '8 min ago' },
    { company: 'NVIDIA', event: 'New SEC filing summarized', timestamp: '14 min ago' }
  ];

  ngOnInit(): void {
    if (this.authService.hasToken()) {
      this.loadCurrentUser();
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
          this.loadCurrentUser();
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        }
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
          this.registerForm.reset({ firstName: '', lastName: '', email: '', password: '' });
          this.loadCurrentUser();
        },
        error: (error) => {
          this.authError = this.extractErrorMessage(error);
        }
      });
  }

  protected loadCurrentUser(): void {
    this.isLoading = true;
    this.authError = '';

    this.authService
      .me()
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
        },
        error: (error) => {
          this.currentUser = null;
          this.authResponse = null;
          this.authService.logout();
          this.authError = this.extractErrorMessage(error);
        }
      });
  }

  protected logout(): void {
    this.authService.logout();
    this.currentUser = null;
    this.authResponse = null;
    this.authMessage = 'Signed out';
    this.authError = '';
  }

  protected hasLoginFieldError(fieldName: 'email' | 'password'): boolean {
    const field = this.loginForm.controls[fieldName];
    return Boolean(field.invalid && (field.dirty || field.touched));
  }

  protected hasRegisterFieldError(
    fieldName: 'firstName' | 'lastName' | 'email' | 'password'
  ): boolean {
    const field = this.registerForm.controls[fieldName];
    return Boolean(field && field.invalid && (field.dirty || field.touched));
  }

  private extractErrorMessage(error: { error?: unknown; message?: string }): string {
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }

    return error.message ?? 'Something went wrong while calling the API';
  }

  trackByTicker(_: number, item: WatchlistItem): string {
    return item.ticker;
  }

  trackByActivity(_: number, item: ActivityItem): string {
    return `${item.company}-${item.timestamp}`;
  }
}
