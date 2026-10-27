import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../service/auth-service';
import { UserService } from '../../../core/services/user-service/user.service';
import { TOKEN_KEY } from '../../../core/const/variables';
import { ToastService } from '../../../core/services/toast-service/toast.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-login',
  styleUrl: './login.css',
  templateUrl: './login.html',
})
export class Login {
  private fb = inject(FormBuilder);
  private autService = inject(AuthService)
  private userService = inject(UserService);
  private router = inject(Router);
  private toastService = inject(ToastService)

  showPassword = signal(false);
  backendError = signal<string | null>(null);


  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  togglePassword(): void {
    this.showPassword.update(value => !value);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.autService.loginRequest(this.loginForm.value).subscribe({
      next: (res) => {
        this.userService.SetCurrentUser(res);
        this.userService.setlocalStorage(TOKEN_KEY, res.token);
        this.router.navigate(['/']);
        this.toastService.show("Welcome Back!", "success", 3500)
      },
      error:(err)=>{


        this.userService.SetCurrentUser(null)
        if ((err.status === 400 || err.status === 404) && err.error) {
          this.backendError.set("The identifications are incorrect.");
        }
      }
    })

  }
}
