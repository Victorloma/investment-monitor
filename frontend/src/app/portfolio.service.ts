import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { apiBaseUrl } from './runtime-config';

export type PortfolioEntry = {
  id: string;
  ticker: string;
  companyName: string;
  irUrl: string | null;
  alertThreshold: number | null;
  monitored: boolean;
  addedAt: string;
};

export type CreatePortfolioEntryRequest = {
  ticker: string;
  companyName: string;
  irUrl: string | null;
  alertThreshold: number | null;
  monitored: boolean;
};

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiBaseUrl = apiBaseUrl;

  list(): Observable<PortfolioEntry[]> {
    return this.http.get<PortfolioEntry[]>(`${this.apiBaseUrl}/portfolio`, {
      headers: this.createAuthHeaders(),
    });
  }

  create(payload: CreatePortfolioEntryRequest): Observable<PortfolioEntry> {
    return this.http.post<PortfolioEntry>(
      `${this.apiBaseUrl}/portfolio`,
      payload,
      {
        headers: this.createAuthHeaders(),
      },
    );
  }

  remove(portfolioEntryId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBaseUrl}/portfolio/${portfolioEntryId}`,
      {
        headers: this.createAuthHeaders(),
      },
    );
  }

  private createAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }
}
