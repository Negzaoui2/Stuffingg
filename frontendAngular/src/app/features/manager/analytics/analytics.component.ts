import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css'],
})
export class AnalyticsComponent implements OnInit {
  qlikAppUrl: SafeResourceUrl | null = null;
  isLoading = true;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.loadQlikApp();
  }

  private loadQlikApp(): void {
    // Chemin du fichier Qlik
    const qlikFilePath = 'C:\\Users\\mnegzaoui\\Documents\\Qlik\\Sense\\Apps\\med-negzaoui-stuffing.qvf';

    // Encoder le chemin du fichier
    const encodedPath = encodeURIComponent(qlikFilePath);

    // Construire l'URL complète
    const qlikUrl = `http://localhost:4848/sense/app/${encodedPath}`;

    // Bypasser la sécurité avec DomSanitizer
    this.qlikAppUrl = this.sanitizer.bypassSecurityTrustResourceUrl(qlikUrl);

    // Simuler le chargement et marquer comme chargé après 1 seconde
    setTimeout(() => {
      this.isLoading = false;
    }, 1000);
  }
}
