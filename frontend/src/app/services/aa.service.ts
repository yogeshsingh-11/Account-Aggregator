import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ConsentResponse, ConsentStatusResponse, CreateConsentRequest } from '../models/consent.model';

@Injectable({ providedIn: 'root' })
export class AaService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  createConsent(request: CreateConsentRequest): Observable<ConsentResponse> {
    return this.http.post<ConsentResponse>(`${this.baseUrl}/consents`, request);
  }

  getConsentStatus(consentRecordId: string): Observable<ConsentStatusResponse> {
    return this.http.get<ConsentStatusResponse>(`${this.baseUrl}/consents/${consentRecordId}/status`);
  }
}
