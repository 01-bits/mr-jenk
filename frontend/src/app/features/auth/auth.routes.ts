import { Routes } from '@angular/router';
import { Register } from './register/register';
import { Login } from './login/login';


export const routes: Routes = [
    {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
    },
    {
        path: 'register',
        component: Register,
        title: 'register',
    },
    {
        path: 'login',
        component: Login,
        title: 'login',
    },
]