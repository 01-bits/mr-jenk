import { inject, Service, signal } from '@angular/core';
import { User } from '../../shared/interfaces/UserInterface';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../const/variables';
import { catchError, of, tap } from 'rxjs';

@Service()
export class UserService {
  currentUser = signal<User | undefined | null>(undefined);
  http = inject(HttpClient);


  SetCurrentUser(user: User | undefined | null) {
    this.currentUser.set(user);
  }

  setlocalStorage(key: string, data: String | User | undefined | null) {
    if (data) {
      if (typeof data === 'string') {
        localStorage.setItem(key, data);
      } else {
        localStorage.setItem(key, JSON.stringify(data));
      }
    } else {
      localStorage.removeItem(key);
    }
  }

  getlocalStorage(key: string) {
    const data = localStorage.getItem(key);
    return data ? data : null;
  }

  isLogged() {
    return this.http.get<User>(`${API_URL}/profile/me`).pipe(
      tap(user => {
        this.SetCurrentUser(user)
      }),
      catchError(() => {
        this.currentUser.set(null)
        return of(null)
      })
    )
  }
}
