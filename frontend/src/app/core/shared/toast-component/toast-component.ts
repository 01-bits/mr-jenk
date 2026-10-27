import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast-service/toast.service';
import { CommonModule } from '@angular/common';

@Component({
  imports: [CommonModule],
  selector: 'app-toast-component',
  styleUrl: './toast-component.css',
  templateUrl: './toast-component.html',
})
export class ToastComponent {
  toastService = inject(ToastService);
}
