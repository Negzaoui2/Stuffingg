import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
})
export class SettingsComponent {
  notificationTypes = [
    { event: "Demande d'accès reçue", inApp: true, email: false },
    { event: 'Compte approuvé', inApp: true, email: true },
    { event: 'Compte rejeté', inApp: true, email: true },
    { event: 'Congé soumis', inApp: true, email: false },
    { event: 'Congé approuvé', inApp: true, email: false },
    { event: 'Congé rejeté', inApp: true, email: false },
    { event: 'Nouvelle affectation', inApp: true, email: false },
  ];

  roles = ['ADMIN', 'DELIVERY_MANAGER', 'COLLABORATEUR'];
}
