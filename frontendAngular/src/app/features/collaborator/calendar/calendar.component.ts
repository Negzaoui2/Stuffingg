import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventClickArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { CollaboratorService } from '../../../core/services/collaborator.service';
import { CollaboratorCalendarEvent } from '../../../core/models/collaborator.model';

@Component({
  selector: 'app-collab-calendar',
  standalone: true,
  imports: [CommonModule, FullCalendarModule],
  templateUrl: './calendar.component.html',
  styleUrls: ['./calendar.component.css'],
})
export class CollabCalendarComponent implements OnInit {
  @ViewChild('calendar') calendarRef!: FullCalendarComponent;

  loading = true;
  error = false;
  showModal = false;
  selectedEvent: { title: string; start: string; end: string; props: any } | null = null;
  currentTitle = '';

  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: 'fr',
    headerToolbar: false,
    height: 'auto',
    nowIndicator: true,
    eventClick: this.onEventClick.bind(this),
    datesSet: (arg) => { this.currentTitle = arg.view.title; },
    events: [],
    eventDidMount: (arg) => {
      arg.el.style.borderRadius = '6px';
      arg.el.style.fontSize = '12px';
      arg.el.style.fontWeight = '500';
      arg.el.style.cursor = 'pointer';
    },
  };

  constructor(private svc: CollaboratorService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.loadEvents(); }

  loadEvents(): void {
    this.loading = true;
    this.error = false;
    const now = new Date();
    const start = new Date(now.getFullYear(), 0, 1).toISOString().split('T')[0];
    const end = new Date(now.getFullYear() + 1, 0, 1).toISOString().split('T')[0];
    this.svc.getCalendarEvents(start, end).subscribe({
      next: (events) => {
        this.calendarOptions = { ...this.calendarOptions, events };
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.error = true; this.loading = false; this.cdr.detectChanges(); },
    });
  }

  prev(): void { this.calendarRef?.getApi()?.prev(); }
  next(): void { this.calendarRef?.getApi()?.next(); }
  today(): void { this.calendarRef?.getApi()?.today(); }

  onEventClick(info: EventClickArg): void {
    const ev = info.event;
    this.selectedEvent = { title: ev.title, start: ev.startStr, end: ev.endStr, props: ev.extendedProps };
    this.showModal = true;
  }

  closeModal(): void { this.showModal = false; this.selectedEvent = null; }

  getTypeLabel(type: string): string { return type === 'ASSIGNMENT' ? 'Affectation' : 'Congé'; }
  getStatusLabel(s: string): string {
    const m: Record<string, string> = { ACTIVE: 'En cours', APPROVED: 'Approuvé', PENDING: 'En attente', REJECTED: 'Refusé', COMPLETED: 'Terminé' };
    return m[s] ?? s;
  }
}
