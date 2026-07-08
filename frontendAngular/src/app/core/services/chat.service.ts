import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import {
  ChatRequest,
  ChatResponse,
  HistoryMessage,
} from '../models/chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  /** URL du microservice chatbot */
  private readonly chatbotUrl = 'http://localhost:8000';

  /** Session ID pour la mémoire conversationnelle */
  private sessionId = this.generateSessionId();

  /** Historique local des messages (signal) */
  readonly messages = signal<HistoryMessage[]>([]);

  /** État de chargement */
  readonly loading = signal(false);

  /** Nombre de messages */
  readonly messageCount = computed(() => this.messages().length);

  constructor(private http: HttpClient) {}

  /** Envoie un message au chatbot et ajoute la réponse */
  sendMessage(text: string): void {
    const trimmed = text.trim();
    if (!trimmed || this.loading()) return;

    // Ajouter le message utilisateur
    this.messages.update((msgs) => [
      ...msgs,
      { role: 'user' as const, content: trimmed },
    ]);

    this.loading.set(true);

    const body: ChatRequest = {
      message: trimmed,
      session_id: this.sessionId,
      history: [],
    };

    this.http.post<ChatResponse>(`${this.chatbotUrl}/chat`, body).subscribe({
      next: (res) => {
        console.log('[ChatService] download_url reçu:', res.download_url);
        this.messages.update((msgs) => [
          ...msgs,
          { role: 'assistant' as const, content: res.answer, downloadUrl: res.download_url },
        ]);
        if (res.session_id) {
          this.sessionId = res.session_id;
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('[ChatService] Erreur:', err);
        this.messages.update((msgs) => [
          ...msgs,
          {
            role: 'assistant' as const,
            content:
              'Désolé, le service est temporairement indisponible. Veuillez réessayer.',
          },
        ]);
        this.loading.set(false);
      },
    });
  }

  /** Réinitialise la conversation */
  clearChat(): void {
    this.messages.set([]);
    this.sessionId = this.generateSessionId();
  }

  private generateSessionId(): string {
    return 'session-' + crypto.randomUUID();
  }
}
