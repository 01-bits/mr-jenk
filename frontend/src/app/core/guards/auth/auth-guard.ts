import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserService } from '../../services/user-service/user.service';
import { TOKEN_KEY } from '../../const/variables';
import { ToastComponent } from '../../shared/toast-component/toast-component';
import { ToastService } from '../../services/toast-service/toast.service';

export const authGuard: CanActivateFn = (route, state) => {
  const userService = inject(UserService);
  const router = inject(Router)
  const toast=inject(ToastService)
  if (userService.getlocalStorage(TOKEN_KEY) === "" || !userService.currentUser()) {
    router.navigateByUrl('/auth/login')
    toast.show("Login required to access this feature.",'info',3500)
    return false
  }

  return true;
};
