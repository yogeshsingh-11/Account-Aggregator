import { Routes } from '@angular/router';
import { ConsentFormComponent } from './consent-form/consent-form.component';
import { ConsentStatusComponent } from './consent-status/consent-status.component';

export const routes: Routes = [
  { path: '', component: ConsentFormComponent },
  { path: 'consents/:id/status', component: ConsentStatusComponent },
  { path: '**', redirectTo: '' }
];
