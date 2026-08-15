export interface CreateConsentRequest {
  customerRef: string;
  purposeCode: string;
  fiTypes: string[];
  dataRangeFrom: string; // ISO 8601
  dataRangeTo: string;   // ISO 8601
  consentExpiry?: string;
}

export interface ConsentResponse {
  consentRecordId: string;
  consentHandle: string;
  redirectUrl: string | null;
  status: ConsentStatus;
  createdAt: string;
}

export interface ConsentStatusResponse {
  consentRecordId: string;
  consentHandle: string;
  consentId: string | null;
  status: ConsentStatus;
  consentExpiry: string | null;
  updatedAt: string;
}

export type ConsentStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'EXPIRED' | 'REVOKED' | 'FAILED';
