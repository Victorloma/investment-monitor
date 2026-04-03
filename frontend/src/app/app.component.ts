import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatToolbarModule } from '@angular/material/toolbar';

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
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatIconModule,
    MatListModule,
    MatProgressBarModule,
    MatToolbarModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  readonly portfolioCount = 12;
  readonly unreadUpdates = 8;
  readonly refreshProgress = 68;

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

  trackByTicker(_: number, item: WatchlistItem): string {
    return item.ticker;
  }

  trackByActivity(_: number, item: ActivityItem): string {
    return `${item.company}-${item.timestamp}`;
  }
}
