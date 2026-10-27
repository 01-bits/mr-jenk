import { Component, HostListener, inject, signal } from '@angular/core';
import { PageMetadata, ProductResponse } from '../../../../core/shared/interfaces/Product';
import { ProductService } from '../../../../core/services/product-service/product.service';
import { API_URL } from '../../../../core/const/variables';
import { ToastService } from '../../../../core/services/toast-service/toast.service';

@Component({
  imports: [],
  selector: 'app-products-page',
  styleUrl: './products-page.css',
  templateUrl: './products-page.html',
})
export class ProductsPage {
  productService = inject(ProductService);
  toastService = inject(ToastService);
  products = this.productService.allProducts;
  pageMetadata?: PageMetadata;
  currentPage: number = 0;
  pageSize: number = 20;
  isLoading: boolean = false;
  hasMoreData: boolean = true;

  ngOnInit(): void {
    this.products.set([]);
    this.fetchProducts();
  }

  fetchProducts(): void {
    if (this.isLoading || !this.hasMoreData) return;

    this.isLoading = true;
    this.productService.getProducts(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.products.set([...this.products(), ...response.content]);
        this.pageMetadata = response.page;
        this.hasMoreData = this.currentPage < response.page.totalPages - 1;
        this.isLoading = false;
      },
      error: (err) => {
        this.toastService.show(err.error?.message || 'Failed to fetch products', 'error', 3500);
        this.isLoading = false;
      },
    });
  }
  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    const pos =
      (document.documentElement.scrollTop || document.body.scrollTop) + window.innerHeight;
    const max = document.documentElement.scrollHeight;

    if (pos >= max - 100) {
      if (this.hasMoreData && !this.isLoading) {
        this.currentPage++;
        this.fetchProducts();
      }
    }
  }

  nextMedia(product: ProductResponse): void {
    if (product.currentMediaIndex === undefined) product.currentMediaIndex = 0;
    if (product.currentMediaIndex < product.imageUrls.length - 1) {
      product.currentMediaIndex++;
    } else {
      product.currentMediaIndex = 0;
    }
  }

  prevMedia(product: ProductResponse): void {
    if (product.currentMediaIndex === undefined) product.currentMediaIndex = 0;
    if (product.currentMediaIndex > 0) {
      product.currentMediaIndex--;
    } else {
      product.currentMediaIndex = product.imageUrls.length - 1;
    }
  }

  getMediaUrl(imageUrl: string): string {
    return API_URL + imageUrl;
  }
}
