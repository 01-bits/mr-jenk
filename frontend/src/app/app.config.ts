import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AuthInterceptor } from './core/interceptor/authInterceptor';
import { UserService } from './core/services/user-service/user.service';
import { firstValueFrom } from 'rxjs';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([AuthInterceptor])),
    provideRouter(routes),
    provideAppInitializer(() => {
      const userService = inject(UserService)
      return firstValueFrom(userService.isLogged())
    }
    )
  ]
};
