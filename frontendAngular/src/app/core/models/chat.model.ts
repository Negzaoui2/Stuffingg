/** Modèles de données pour le chatbot RH */

export interface HistoryMessage {
  role: 'user' | 'assistant';
  content: string;
  downloadUrl?: string | null;
}

export interface ChatRequest {
  message: string;
  session_id: string | null;
  history: HistoryMessage[];
}

export interface RetrievedItem {
  id: string;
  score: number;
  text: string;
  metadata: Record<string, unknown>;
}

export interface ChatResponse {
  answer: string;
  intent: string;
  retrieved: RetrievedItem[];
  used_llm: boolean;
  session_id: string | null;
  download_url: string | null;
}
