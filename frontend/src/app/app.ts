import { UserService } from './core/services/user-service/user.service';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './core/shared/toast-component/toast-component';
import { ThemeService } from './core/services/theme-servicee/theme.service';

@Component({
  imports: [RouterOutlet, ToastComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App  {
  protected readonly title = signal('Buy-01');
  private readonly themeService = inject(ThemeService);
}
