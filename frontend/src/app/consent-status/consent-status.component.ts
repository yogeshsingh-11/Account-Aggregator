import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import { AaService } from '../services/aa.service';
import { ConsentStatusResponse } from '../models/consent.model';

const POLL_INTERVAL_MS = 4000;
const TERMINAL_STATUSES = ['ACTIVE', 'REJECTED', 'EXPIRED', 'REVOKED', 'FAILED'];

@Component({
  selector: 'app-consent-status',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './consent-status.component.html'
})
export class ConsentStatusComponent implements OnInit, OnDestroy {
  status: ConsentStatusResponse | null = null;
  errorMessage: string | null = null;
  private pollSub?: Subscription;
  private consentRecordId!: string;

  constructor(private route: ActivatedRoute, private aaService: AaService) {}

  ngOnInit(): void {
    this.consentRecordId = this.route.snapshot.paramMap.get('id')!;

    this.pollSub = interval(POLL_INTERVAL_MS)
      .pipe(
        startWith(0),
        switchMap(() => this.aaService.getConsentStatus(this.consentRecordId))
      )
      .subscribe({
        next: (result) => {
          this.status = result;
          this.errorMessage = null;
          if (TERMINAL_STATUSES.includes(result.status) && result.status !== 'ACTIVE') {
            this.stopPolling();
          }
          // Note: once ACTIVE, we keep polling briefly is unnecessary — stop immediately.
          if (result.status === 'ACTIVE') {
            this.stopPolling();
          }
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Could not fetch consent status.';
          this.stopPolling();
        }
      });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
  }
}
