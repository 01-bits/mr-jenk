import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../service/auth-service';
import { HttpErrorResponse } from '@angular/common/http'; // Importez ceci
import { UserService } from '../../../core/services/user-service/user.service';
import { TOKEN_KEY } from '../../../core/const/variables';
import { ToastService } from '../../../core/services/toast-service/toast.service';

type Role = 'CLIENT' | 'SELLER';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-register',
  styleUrl: './register.css',
  templateUrl: './register.html',
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private router = inject(Router);
  private toastService = inject(ToastService)


  showPassword = signal(false);
  selectedRole = signal<Role>('SELLER');

  backendError = signal<string | null>(null);

  registerForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    role: ['SELLER', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  togglePassword(): void {
    this.showPassword.update(value => !value);
  }

  selectRole(role: Role): void {
    this.selectedRole.set(role);
    this.registerForm.get('role')?.setValue(role);
  }

  onSubmit(): void {
    this.backendError.set(null);

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.authService.registerRequest(this.registerForm.value).subscribe({
      next: (res) => {
        this.userService.SetCurrentUser(res);
        this.userService.setlocalStorage(TOKEN_KEY, res.token);
        this.router.navigate(['/']);
        this.toastService.show("Registration successful!", "success", 3500)

      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409 && err.error?.detail) {
          this.backendError.set(err.error.detail);
        }

        else if (err.status === 400 && err.error) {
          Object.keys(err.error).forEach(field => {
            const control = this.registerForm.get(field);
            if (control) {
              control.setErrors({ serverError: err.error[field] });
              control.markAsTouched();
            }
          });
        } else {
          this.toastService.show("Something went wrong", "success", 3500)

        }
      }
    });
  }
}
