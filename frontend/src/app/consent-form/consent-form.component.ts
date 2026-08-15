import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AaService } from '../services/aa.service';
import { CreateConsentRequest } from '../models/consent.model';

const FI_TYPE_OPTIONS = ['DEPOSIT', 'TERM_DEPOSIT', 'RECURRING_DEPOSIT', 'LOAN'];

@Component({
  selector: 'app-consent-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './consent-form.component.html'
})
export class ConsentFormComponent {
  fiTypeOptions = FI_TYPE_OPTIONS;

  customerRef = '';
  purposeCode = 'CREDIT_UNDERWRITING';
  selectedFiTypes: string[] = ['DEPOSIT'];
  dataRangeFromDays = 90; // fetch last N days of data
  submitting = false;
  errorMessage: string | null = null;

  constructor(private aaService: AaService, private router: Router) {}

  toggleFiType(type: string, checked: boolean) {
    if (checked) {
      this.selectedFiTypes = [...this.selectedFiTypes, type];
    } else {
      this.selectedFiTypes = this.selectedFiTypes.filter((t) => t !== type);
    }
  }

  submit() {
    this.errorMessage = null;

    if (!this.customerRef.trim()) {
      this.errorMessage = 'Customer reference is required.';
      return;
    }
    if (this.selectedFiTypes.length === 0) {
      this.errorMessage = 'Select at least one FI type.';
      return;
    }

    const now = new Date();
    const from = new Date(now.getTime() - this.dataRangeFromDays * 24 * 60 * 60 * 1000);
    const to = new Date(now.getTime() + 24 * 60 * 60 * 1000); // must be in the future per API contract

    const request: CreateConsentRequest = {
      customerRef: this.customerRef.trim(),
      purposeCode: this.purposeCode,
      fiTypes: this.selectedFiTypes,
      dataRangeFrom: from.toISOString(),
      dataRangeTo: to.toISOString()
    };

    this.submitting = true;
    this.aaService.createConsent(request).subscribe({
      next: (response) => {
        this.submitting = false;
        if (response.redirectUrl) {
          // Present the AA consent approval screen to the customer.
          window.location.href = response.redirectUrl;
        } else {
          this.router.navigate(['/consents', response.consentRecordId, 'status']);
        }
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Failed to create consent. Please try again.';
      }
    });
  }
}
