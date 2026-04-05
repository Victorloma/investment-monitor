import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { apiBaseUrl } from './runtime-config';

export type BriefingItem = {
  type: 'EARNINGS' | 'FILING' | 'PRESS_RELEASE' | 'EVENT' | 'OTHER';
  title: string;
  date: string | null;
  summary: string;
  url: string | null;
};

export type BriefingContent = {
  asOf: string;
  analystSummary: string;
  items: BriefingItem[];
};

export type IrBriefingResponse = {
  briefingId: string;
  portfolioEntryId: string;
  briefingJson: string;
  fetchedAt: string;
};

export type UserSettingsResponse = {
  hasApiKey: boolean;
  providerUrl: string;
  modelId: string;
};

export type SaveSettingsRequest = {
  apiKey: string;
  providerUrl: string;
  modelId: string;
};

@Injectable({ providedIn: 'root' })
export class BriefingService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiBaseUrl = apiBaseUrl;

  getSettings(): Observable<UserSettingsResponse> {
    return this.http.get<UserSettingsResponse>(`${this.apiBaseUrl}/settings`, {
      headers: this.authHeaders(),
    });
  }

  saveSettings(request: SaveSettingsRequest): Observable<void> {
    return this.http.put<void>(
      `${this.apiBaseUrl}/settings`,
      request,
      { headers: this.authHeaders() },
    );
  }

  getLatestBriefing(portfolioEntryId: string): Observable<IrBriefingResponse | null> {
    return this.http.get<IrBriefingResponse | null>(
      `${this.apiBaseUrl}/portfolio/${portfolioEntryId}/briefing`,
      { headers: this.authHeaders() },
    );
  }

  fetchBriefing(portfolioEntryId: string): Observable<IrBriefingResponse> {
    return this.http.post<IrBriefingResponse>(
      `${this.apiBaseUrl}/portfolio/${portfolioEntryId}/briefing`,
      null,
      { headers: this.authHeaders() },
    );
  }

  refreshAll(): Observable<IrBriefingResponse[]> {
    return this.http.post<IrBriefingResponse[]>(
      `${this.apiBaseUrl}/portfolio/briefing/refresh-all`,
      null,
      { headers: this.authHeaders() },
    );
  }

  parseBriefingJson(briefingJson: string): BriefingContent | null {
    try {
      // Strip markdown code fences if the model wrapped the JSON
      const cleaned = briefingJson
        .replace(/^```(?:json)?\s*/i, '')
        .replace(/\s*```\s*$/, '')
        .trim();
      return JSON.parse(cleaned) as BriefingContent;
    } catch {
      return null;
    }
  }

  private authHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }
}
