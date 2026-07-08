import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
})
export class RegisterComponent {
  currentStep = 1;
  totalSteps = 2; // ← 2 étapes au lieu de 3

  requestForm: FormGroup;
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: Auth,
    private router: Router
  ) {
    this.requestForm = this.fb.group({
      // Étape 1 : Informations personnelles
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],

      // Étape 2 : Informations professionnelles + Message
      company: ['', [Validators.required]],
      jobTitle: ['', [Validators.required]],
      message: ['', [Validators.maxLength(4000)]]
    });
  }

  // Navigation entre les étapes
  nextStep(): void {
    if (this.isStepValid(this.currentStep)) {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
      }
    } else {
      this.markStepAsTouched(this.currentStep);
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  goToStep(step: number): void {
    if (step < this.currentStep) {
      this.currentStep = step;
    }
  }

  // Validation par étape
  isStepValid(step: number): boolean {
    const fields = this.getStepFields(step);
    return fields.every(field => {
      const control = this.requestForm.get(field);
      return control?.valid || false;
    });
  }

  getStepFields(step: number): string[] {
    switch (step) {
      case 1:
        return ['firstName', 'lastName', 'email'];
      case 2:
        return ['company', 'jobTitle'];
      default:
        return [];
    }
  }

  markStepAsTouched(step: number): void {
    const fields = this.getStepFields(step);
    fields.forEach(field => {
      this.requestForm.get(field)?.markAsTouched();
    });
  }

  // Soumission finale
  onSubmit(): void {

      console.log("DATA SENT", this.requestForm.value);

    if (!this.requestForm.valid) {
      this.markFormGroupTouched(this.requestForm);
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.submitAccountRequest(this.requestForm.value).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.isLoading = false;

        setTimeout(() => {
          this.router.navigate(['/auth/login']);
        }, 3000);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Une erreur est survenue';
        console.error('Erreur demande compte', error);
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  // Getters
  get firstName() { return this.requestForm.get('firstName'); }
  get lastName() { return this.requestForm.get('lastName'); }
  get email() { return this.requestForm.get('email'); }
  get phone() { return this.requestForm.get('phone'); }
  get company() { return this.requestForm.get('company'); }
  get jobTitle() { return this.requestForm.get('jobTitle'); }
  get message() { return this.requestForm.get('message'); }

  get progressPercentage(): number {
    return (this.currentStep / this.totalSteps) * 100;
  }
}