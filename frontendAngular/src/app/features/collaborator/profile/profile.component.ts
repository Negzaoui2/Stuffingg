import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CollaboratorService } from '../../../core/services/collaborator.service';
import { CollaboratorProfile, SkillDto, SkillLevel } from '../../../core/models/collaborator.model';
import { Subject, debounceTime, switchMap, of } from 'rxjs';

@Component({
  selector: 'app-collab-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class CollabProfileComponent implements OnInit {
  profile: CollaboratorProfile | null = null;
  isLoading = true;

  // Phone edit
  editingPhone = false;
  phoneValue = '';
  savingPhone = false;

  // Skills edit
  editingSkills = false;
  skillsDraft: SkillDto[] = [];
  savingSkills = false;
  skillLevels: SkillLevel[] = ['BEGINNER', 'INTERMEDIATE', 'EXPERT'];

  // Autocomplete
  suggestions: string[] = [];
  newSkillName = '';
  searchSubject = new Subject<string>();

  constructor(private svc: CollaboratorService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
    this.searchSubject.pipe(
      debounceTime(300),
      switchMap(q => q.length >= 2 ? this.svc.getSkillSuggestions(q) : of([])),
    ).subscribe(s => this.suggestions = s);
  }

  load(): void {
    this.isLoading = true;
    this.svc.getProfile().subscribe({
      next: (p) => { this.profile = p; this.isLoading = false; this.cdr.detectChanges(); },
      error: () => { this.isLoading = false; this.cdr.detectChanges(); },
    });
  }

  // ─── Phone ──────────────────────────
  startPhoneEdit(): void {
    this.phoneValue = this.profile?.phone ?? '';
    this.editingPhone = true;
  }

  cancelPhoneEdit(): void { this.editingPhone = false; }

  savePhone(): void {
    this.savingPhone = true;
    this.svc.updateProfile({ phone: this.phoneValue }).subscribe({
      next: (p) => { this.profile = p; this.editingPhone = false; this.savingPhone = false; this.cdr.detectChanges(); },
      error: () => { this.savingPhone = false; this.cdr.detectChanges(); },
    });
  }

  // ─── Skills ─────────────────────────
  startSkillsEdit(): void {
    this.skillsDraft = (this.profile?.skills ?? []).map(s => ({ ...s }));
    this.editingSkills = true;
  }

  cancelSkillsEdit(): void { this.editingSkills = false; this.suggestions = []; this.newSkillName = ''; }

  removeSkill(i: number): void { this.skillsDraft.splice(i, 1); }

  onSkillSearch(q: string): void { this.searchSubject.next(q); }

  selectSuggestion(name: string): void {
    if (!this.skillsDraft.some(s => s.name === name)) {
      this.skillsDraft.push({ name, level: 'BEGINNER' });
    }
    this.newSkillName = '';
    this.suggestions = [];
  }

  addCustomSkill(): void {
    const name = this.newSkillName.trim();
    if (name && !this.skillsDraft.some(s => s.name === name)) {
      this.skillsDraft.push({ name, level: 'BEGINNER' });
    }
    this.newSkillName = '';
    this.suggestions = [];
  }

  saveSkills(): void {
    this.savingSkills = true;
    const payload = this.skillsDraft.map(s => ({ name: s.name, level: s.level }));
    this.svc.updateSkills(payload).subscribe({
      next: (p) => { this.profile = p; this.editingSkills = false; this.savingSkills = false; this.cdr.detectChanges(); },
      error: () => { this.savingSkills = false; this.cdr.detectChanges(); },
    });
  }

  getLevelLabel(l: string): string {
    const m: Record<string, string> = { BEGINNER: 'Débutant', INTERMEDIATE: 'Intermédiaire', EXPERT: 'Expert' };
    return m[l] ?? l;
  }

  getLevelClass(l: string): string {
    const m: Record<string, string> = { BEGINNER: 'lvl-beginner', INTERMEDIATE: 'lvl-intermediate', EXPERT: 'lvl-expert' };
    return m[l] ?? '';
  }
}
