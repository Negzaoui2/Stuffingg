import {
  Component,
  ViewChild,
  ElementRef,
  AfterViewChecked,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { ChatService } from '../../core/services/chat.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.css',
})
export class ChatbotComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  /** URL de base du backend chatbot */
  readonly backendUrl = 'http://localhost:8000';

  /** Message en cours de saisie */
  userInput = signal('');

  /** Suggestions rapides affichées au démarrage */
  readonly suggestions = [
    'Combien de départements y a-t-il ?',
    'Taux d\'attrition par département',
    'Qui est disponible pour un projet Java ?',
  ];

  constructor(readonly chat: ChatService) {}

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  send(): void {
    const msg = this.userInput();
    if (!msg.trim()) return;
    this.chat.sendMessage(msg);
    this.userInput.set('');
  }

  sendSuggestion(text: string): void {
    this.chat.sendMessage(text);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  clearChat(): void {
    this.chat.clearChat();
  }

  openDownload(downloadUrl: string): void {
    window.open(this.backendUrl + downloadUrl, '_blank');
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    } catch (_) {}
  }
}
