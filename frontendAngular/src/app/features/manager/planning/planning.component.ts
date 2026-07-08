import { Component, OnInit, ViewChild, signal, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventClickArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import resourceTimelinePlugin from '@fullcalendar/resource-timeline';
import interactionPlugin from '@fullcalendar/interaction';
import { CalendarService } from '../../../core/services/calendar.service';
import { CalendarEvent, CalendarEventExtendedProps, CalendarResource } from '../../../core/models/calendar.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [CommonModule, FullCalendarModule],
  templateUrl: './planning.component.html',
  styleUrls: ['./planning.component.css'],
})
export class PlanningComponent implements OnInit {
  @ViewChild('calendar') calendarRef!: FullCalendarComponent;

  loading = signal(true);
  error = signal(false);
  activeView = signal<'timeline' | 'month'>('timeline');

  // Event detail modal
  showModal = signal(false);
  selectedEvent = signal<{
    title: string;
    start: string;
    end: string;
    props: CalendarEventExtendedProps;
  } | null>(null);

  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, resourceTimelinePlugin, interactionPlugin],
    initialView: 'resourceTimelineMonth',
    schedulerLicenseKey: 'CC-Attribution-NonCommercial-NoDerivatives',
    headerToolbar: false, // We use our custom header
    locale: 'fr',
    height: 'auto',
    nowIndicator: true,
    editable: false,
    selectable: false,
    resourceAreaHeaderContent: 'Collaborateur',
    resourceAreaWidth: '260px',
    slotMinWidth: 50,
    resourceOrder: 'title',
    eventClick: this.onEventClick.bind(this),
    resources: [],
    events: [],
    resourceLabelDidMount: (arg) => {
      const res = arg.resource;
      const availability = res.extendedProps?.['availability'] as string;
      const department = res.extendedProps?.['department'] as string;

      const el = arg.el;
      el.innerHTML = '';
      el.style.padding = '8px 12px';
      el.style.display = 'flex';
      el.style.alignItems = 'center';
      el.style.gap = '10px';

      // Availability dot
      const dot = document.createElement('span');
      dot.style.width = '10px';
      dot.style.height = '10px';
      dot.style.borderRadius = '50%';
      dot.style.flexShrink = '0';
      if (availability === 'AVAILABLE') {
        dot.style.background = '#10b981';
      } else if (availability === 'SOON_AVAILABLE') {
        dot.style.background = '#f59e0b';
      } else {
        dot.style.background = '#ef4444';
      }
      el.appendChild(dot);

      const textWrap = document.createElement('div');
      textWrap.style.display = 'flex';
      textWrap.style.flexDirection = 'column';
      textWrap.style.lineHeight = '1.3';

      const nameSpan = document.createElement('span');
      nameSpan.textContent = res.title;
      nameSpan.style.fontWeight = '600';
      nameSpan.style.fontSize = '13px';
      nameSpan.style.color = '#1e293b';
      textWrap.appendChild(nameSpan);

      if (department) {
        const deptSpan = document.createElement('span');
        deptSpan.textContent = department;
        deptSpan.style.fontSize = '11px';
        deptSpan.style.color = '#94a3b8';
        textWrap.appendChild(deptSpan);
      }

      el.appendChild(textWrap);
    },
    eventDidMount: (arg) => {
      arg.el.style.borderRadius = '6px';
      arg.el.style.fontSize = '12px';
      arg.el.style.fontWeight = '500';
      arg.el.style.cursor = 'pointer';
    },
  };

  constructor(private calendarService: CalendarService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(false);

    const now = new Date();
    const start = new Date(now.getFullYear(), 0, 1).toISOString().split('T')[0];
    const end = new Date(now.getFullYear() + 1, 0, 1).toISOString().split('T')[0];

    forkJoin({
      resources: this.calendarService.getResources(),
      events: this.calendarService.getEvents(start, end),
    }).subscribe({
      next: ({ resources, events }) => {
        const fcResources = resources.map((r: CalendarResource) => ({
          id: r.id,
          title: r.title,
          extendedProps: { department: r.department, availability: r.availability },
        }));

        // Filter only assignments for the timeline, keep all for month view
        const assignmentEvents = events.filter(
          (e: CalendarEvent) => e.extendedProps.type === 'ASSIGNMENT'
        );

        this.calendarOptions = {
          ...this.calendarOptions,
          resources: fcResources,
          events: assignmentEvents,
        };
        this.loading.set(false);
        this.cdr.detectChanges();
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
    });
  }

  switchView(view: 'timeline' | 'month'): void {
    this.activeView.set(view);
    const api = this.calendarRef?.getApi();
    if (!api) return;
    api.changeView(view === 'timeline' ? 'resourceTimelineMonth' : 'dayGridMonth');
  }

  prev(): void {
    this.calendarRef?.getApi()?.prev();
  }

  next(): void {
    this.calendarRef?.getApi()?.next();
  }

  today(): void {
    this.calendarRef?.getApi()?.today();
  }

  get currentTitle(): string {
    return this.calendarRef?.getApi()?.view?.title ?? '';
  }

  onEventClick(info: EventClickArg): void {
    const ev = info.event;
    this.selectedEvent.set({
      title: ev.title,
      start: ev.startStr,
      end: ev.endStr,
      props: ev.extendedProps as CalendarEventExtendedProps,
    });
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.selectedEvent.set(null);
  }

  getStatusLabel(status: string | undefined): string {
    const map: Record<string, string> = {
      ACTIVE: 'En cours',
      COMPLETED: 'Terminé',
      ON_HOLD: 'En pause',
      PLANNED: 'Planifié',
      ASSIGNED: 'Affecté',
    };
    return map[status ?? ''] ?? status ?? '';
  }

  getStatusClass(status: string | undefined): string {
    const map: Record<string, string> = {
      ACTIVE: 'badge-active',
      COMPLETED: 'badge-completed',
      ON_HOLD: 'badge-hold',
      PLANNED: 'badge-planned',
      ASSIGNED: 'badge-active',
    };
    return map[status ?? ''] ?? '';
  }
}
