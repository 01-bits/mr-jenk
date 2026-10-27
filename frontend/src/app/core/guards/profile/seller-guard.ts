import { CanActivateFn, Router } from '@angular/router';
import { UserService } from '../../services/user-service/user.service';
import { inject } from '@angular/core';
import { ToastService } from '../../services/toast-service/toast.service';

export const sellerGuard: CanActivateFn = (route, state) => {
  const userService = inject(UserService);
  const router = inject(Router)
  const toast = inject(ToastService)
  if (!userService.currentUser() || userService.currentUser()?.role !== "SELLER") {
    router.navigateByUrl('/')
    toast.show("Access denied", 'info', 3500)
    return false
  }


  return true;
};
