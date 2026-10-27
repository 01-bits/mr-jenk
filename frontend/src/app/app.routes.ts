import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth/auth-guard';
import { guestGuard } from './core/guards/auth/guest-guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
    loadChildren: () => import('./features/home/home.routes').then((m) => m.routes),
  },
  {
    path: 'auth',
    loadComponent: () => import('./features/auth/auth').then((m) => m.Auth),
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.routes),
    canActivate: [guestGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
