import { Component } from '@angular/core';
import { HeaderComponent } from './components/header-component/header-component';
import { HeroComponent } from './pages/hero-page/hero-page';
import { RouterOutlet } from '@angular/router';

@Component({
  imports: [HeaderComponent,RouterOutlet],
  selector: 'app-home',
  styleUrl: './home.css',
  templateUrl: './home.html',
})
export class Home {}
