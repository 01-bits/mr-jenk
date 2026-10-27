import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  imports: [CommonModule,RouterLink],
  selector: 'app-hero-page',
  styleUrl: './hero-page.css',
  templateUrl: './hero-page.html',
})
export class HeroComponent {}
