import { Service, signal } from '@angular/core';
import { Toast, ToastType } from '../../shared/interfaces/ToastInterface';

@Service()
export class ToastService {
  toasts = signal<Toast[]>([]);
  private counter = 0;

  show(message: string, type: ToastType = 'success', duration: number = 4000) {
    const id = ++this.counter;
    const newToast: Toast = { id, message, type };

    // Ajoute le toast à la liste
    this.toasts.update(current => [...current, newToast]);

    // Suppression automatique après le délai
    setTimeout(() => {
      this.remove(id);
    }, duration);
  }

  remove(id: number) {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }
}
