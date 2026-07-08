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
  selector: 'app-chatbot-bubble',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot-bubble.component.html',
  styleUrls: ['./chatbot-bubble.component.css'],
})
export class ChatbotBubbleComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  isOpen = false;
  userInput = signal('');

  readonly suggestions = [
    'Qui est disponible pour un projet Java ?',
    'Taux d\'occupation de l\'équipe',
    'Combien de collaborateurs en congé ?',
  ];

  constructor(readonly chat: ChatService) {}

  ngAfterViewChecked(): void {
    if (this.isOpen) this.scrollToBottom();
  }

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  close(): void {
    this.isOpen = false;
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

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch (_) {}
  }
}
