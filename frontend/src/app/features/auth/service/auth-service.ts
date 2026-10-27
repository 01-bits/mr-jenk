import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { API_URL } from '../../../core/const/variables';
import { User } from '../../../core/shared/interfaces/UserInterface';

@Service()
export class AuthService {
  http=inject(HttpClient);


  registerRequest(data: any) {
    return this.http.post<User>(`${API_URL}/auth/register`, data);
  }

  loginRequest(data: any) {
    return this.http.post<User>(`${API_URL}/auth/login`, data);
  }

}
