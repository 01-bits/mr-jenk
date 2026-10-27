import { Routes } from '@angular/router';
import { HeroComponent } from './pages/hero-page/hero-page';
import { ProfilePage } from './pages/profile-page/profile-page';
import { authGuard } from '../../core/guards/auth/auth-guard';
import { sellerGuard } from '../../core/guards/profile/seller-guard';
import { DashboardPage } from './pages/dashboard-page/dashboard-page';
import { ProductsPage } from './pages/products-page/products-page';

export const routes: Routes = [
  {
    path: '',
    component: HeroComponent,
  },
  {
    path: 'profile',
    title: 'profile',
    component: ProfilePage,
    canActivate: [authGuard, sellerGuard],
  },
  {
    path: 'dashboard',
    title: 'dashboard',
    component: DashboardPage,
    canActivate: [authGuard, sellerGuard],
  },
  {
    path: 'products',
    title: 'products',
    component: ProductsPage,
  },
];
