import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { UserService } from "../services/user-service/user.service";
import { TOKEN_KEY } from "../const/variables";
import { catchError, throwError } from "rxjs";
import { Router } from "@angular/router";


export const AuthInterceptor: HttpInterceptorFn = (req, next) => {
  const userSevice = inject(UserService)
  const token = userSevice.getlocalStorage(TOKEN_KEY) ?? ""
  const router = inject(Router);

  const cloned = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return next(cloned).pipe(
    catchError((err) => {

      if (err.status === 401 && router.url!="/") {
        userSevice.setlocalStorage(TOKEN_KEY, null)
        userSevice.currentUser.set(null)
        router.navigateByUrl('/auth/login')

      }
      return throwError(() => err)
    })
  );
};
