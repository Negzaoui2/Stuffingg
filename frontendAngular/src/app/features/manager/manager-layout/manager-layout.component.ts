import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Auth } from '../../../core/services/auth';
import { ManagerService } from '../../../core/services/manager.service';
import { ChatbotBubbleComponent } from '../../../shared/chatbot-bubble/chatbot-bubble.component';

@Component({
  selector: 'app-manager-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ChatbotBubbleComponent],
  templateUrl: './manager-layout.component.html',
  styleUrls: ['./manager-layout.component.css'],
})
export class ManagerLayoutComponent implements OnInit {
  pendingLeavesCount = 0;

  constructor(private auth: Auth, private managerService: ManagerService) {}

  ngOnInit(): void {
    this.loadPendingLeavesCount();
  }

  get userInitials(): string {
    return this.auth.getUserInitials() || 'DM';
  }

  get displayName(): string {
    return this.auth.getFullName() || 'Manager';
  }

  logout(): void {
    this.auth.logout();
  }

  private loadPendingLeavesCount(): void {
    this.managerService.getTeamLeaves('PENDING', 0, 1).subscribe({
      next: (res) => this.pendingLeavesCount = res.total ?? 0,
      error: () => this.pendingLeavesCount = 0,
    });
  }
}
