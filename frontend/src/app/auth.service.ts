import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { apiBaseUrl } from './runtime-config';

export type AuthResponse = {
  userId: string;
  email: string;
  roles: string[];
  accessToken: string;
};

export type CurrentUser = {
  userId: string;
  email: string;
  roles: string[];
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiBaseUrl = apiBaseUrl;
  private readonly tokenStorageKey = 'investment-monitor.access-token';

  constructor(private readonly http: HttpClient) {}

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiBaseUrl}/auth/register`, payload)
      .pipe(tap((response) => this.storeToken(response.accessToken)));
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiBaseUrl}/auth/login`, payload)
      .pipe(tap((response) => this.storeToken(response.accessToken)));
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${this.apiBaseUrl}/users/me`, {
      headers: this.createAuthHeaders(),
    });
  }

  logout(): void {
    localStorage.removeItem(this.tokenStorageKey);
  }

  hasToken(): boolean {
    return Boolean(this.getToken());
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenStorageKey);
  }

  private storeToken(token: string): void {
    localStorage.setItem(this.tokenStorageKey, token);
  }

  private createAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }
}
