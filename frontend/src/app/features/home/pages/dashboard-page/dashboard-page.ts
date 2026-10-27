import { Component, HostListener, Input, inject, signal } from '@angular/core';
import { API_URL } from '../../../../core/const/variables';
import { PageMetadata, ProductResponse } from '../../../../core/shared/interfaces/Product';
import { ProductService } from '../../../../core/services/product-service/product.service';
import { ToastService } from '../../../../core/services/toast-service/toast.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { forkJoin, map, of } from 'rxjs';
import { ValidImage } from '../../../../core/utils/utils';
import { Filevalidator } from '../../../../core/utils/getRealMimeType';

@Component({
  imports: [ReactiveFormsModule, CommonModule],
  selector: 'app-dashboard-page',
  styleUrl: './dashboard-page.css',
  templateUrl: './dashboard-page.html',
})
export class DashboardPage {
  @Input() showTitle = true;
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);
  private getRealMimeType = inject(Filevalidator);
  productService = inject(ProductService);
  products = this.productService.products;
  pageMetadata?: PageMetadata;
  currentPage: number = 0;
  pageSize: number = 20;
  isLoading: boolean = false;
  hasMoreData: boolean = true;
  deletingProduct = signal<ProductResponse | null>(null);
  editingProduct = signal<ProductResponse | null>(null);
  editableMediaUrls = signal<string[]>([]);
  newMediaFiles: File[] = [];
  newMediaPreviews = signal<{ file: File; url: string }[]>([]);
  editProductForm: FormGroup;
  isUpdating = false;
  isDeleting: boolean = false;
  constructor() {
    this.editProductForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(2000)]],
      price: [null, [Validators.required, Validators.min(0.01), Validators.max(999999999)]],
      quantity: [0, [Validators.required, Validators.min(0), Validators.max(2147483647)]],
      media: [[] as string[]],
    });
  }
  ngOnInit(): void {
    this.productService.products.set([]);
    this.fetchProducts();
  }

  fetchProducts(): void {
    if (this.isLoading || !this.hasMoreData) return;

    this.isLoading = true;
    this.productService.getMyProducts(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.productService.products.set([...this.productService.products(), ...response.content]);
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

  openProductMenu(product: ProductResponse): void {
    product.isMenuOpen = !product.isMenuOpen;
  }

  editProduct(product: ProductResponse): void {
    product.isMenuOpen = false;
    this.editingProduct.set(product);
    this.editProductForm.patchValue({
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
      media: [...(product.imageUrls ?? [])],
    });
    this.editableMediaUrls.set([...(product.imageUrls ?? [])]);
  }

  cancelEdit(): void {
    this.clearNewMedia();
    this.editingProduct.set(null);
    this.editableMediaUrls.set([]);
    this.editProductForm.reset({ name: '', description: '', price: null, quantity: 0, media: [] });
  }

  async onNewMediaSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    const availableSlots = 5 - this.editableMediaUrls().length - this.newMediaFiles.length;

    if (files.length > availableSlots) {
      this.toastService.show(
        `You can keep a maximum of 5 images. ${availableSlots} slot(s) remaining.`,
        'error',
        3500,
      );
      input.value = '';
      return;
    }

    for (const file of files) {
      if (file.size > 2 * 1024 * 1024) {
        this.toastService.show(`${file.name} must be an image smaller than 2MB.`, 'error', 3500);
        continue;
      }
      try {
        const realMimeType = await this.getRealMimeType.validateRealMimeType(file);
        if (!ValidImage(realMimeType)) {
          this.toastService.show(`File ${file.name} has an invalid format.`, 'error', 3500);
          continue;
        }

        const url = URL.createObjectURL(file);
        this.newMediaFiles.push(file);
        this.newMediaPreviews.update((previews) => [...previews, { file, url }]);
      } catch (err) {
        this.toastService.show(`Error validating file: ${file.name}`, 'error', 3500);
      }
    }
    input.value = '';
  }

  removeExistingMedia(url: string): void {
    this.editableMediaUrls.update((urls) => urls.filter((item) => item !== url));
    this.editProductForm.patchValue({ media: this.editableMediaUrls() });
  }

  removeNewMedia(index: number): void {
    const preview = this.newMediaPreviews()[index];
    if (!preview) return;
    URL.revokeObjectURL(preview.url);
    this.newMediaFiles.splice(index, 1);
    this.newMediaPreviews.update((previews) =>
      previews.filter((_, itemIndex) => itemIndex !== index),
    );
  }

  private clearNewMedia(): void {
    this.newMediaPreviews().forEach((preview) => URL.revokeObjectURL(preview.url));
    this.newMediaFiles = [];
    this.newMediaPreviews.set([]);
  }

  private mediaIdFromUrl(url: string): string {
    const id = url.split('?')[0].split('/').filter(Boolean).pop();
    if (!id) throw new Error('Invalid media URL');
    return id;
  }

  onUpdateProduct(): void {
    const productToEdit = this.editingProduct();
    if (!productToEdit || this.isUpdating) return;

    if (this.editProductForm.invalid) {
      this.editProductForm.markAllAsTouched();
      return;
    }

    this.isUpdating = true;
    const formValues = this.editProductForm.getRawValue();

    const updatePayload = {
      name: formValues.name,
      description: formValues.description || null,
      price: Number(formValues.price),
      quantity: Number(formValues.quantity),
    };
    const retainedMedia = this.editableMediaUrls();
    const removedMedia = (productToEdit.imageUrls ?? []).filter(
      (url) => !retainedMedia.includes(url),
    );

    this.productService.updateProduct(productToEdit.id, updatePayload).subscribe({
      next: (updatedProduct) => {
        forkJoin({
          deleted: removedMedia.length
            ? forkJoin(
                removedMedia.map((url) =>
                  this.productService.deleteImage(this.mediaIdFromUrl(url)),
                ),
              )
            : of([]),
          uploaded: this.newMediaFiles.length
            ? this.productService.uploadImages(this.newMediaFiles, productToEdit.id)
            : of([]),
        })
          .pipe(
            map(({ uploaded }) => ({
              ...updatedProduct,
              imageUrls: [...retainedMedia, ...uploaded.map((media) => media.url)],
              currentMediaIndex: 0,
              isMenuOpen: false,
            })),
          )
          .subscribe({
            next: (product) => {
              this.products.update((current) =>
                current.map((item) => (item.id === product.id ? product : item)),
              );
              this.productService.allProducts.update((current) =>
                current.map((item) => (item.id === product.id ? product : item)),
              );
              this.cancelEdit();
              this.isUpdating = false;
              this.toastService.show('Product updated successfully', 'success', 3500);
            },
            error: (err) => {
              this.toastService.show(
                err.error?.message || 'Failed to update product media',
                'error',
                3500,
              );
              this.cancelEdit();
              this.isUpdating = false;
            },
          });
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to update product';
        this.toastService.show(msg, 'error', 3500);
        this.cancelEdit();
        this.isUpdating = false;
      },
    });
  }

  deleteProduct(product: ProductResponse): void {
    product.isMenuOpen = false;
    this.deletingProduct.set(product);
  }
  executeDelete(): void {
    if (!this.deletingProduct() || this.isDeleting) return;

    this.isDeleting = true;
    const postId = this.deletingProduct()!.id;

    this.productService.deleteProduct(postId).subscribe({
      next: () => {
        this.productService.products.set(
          this.productService.products().filter((p) => p.id !== postId),
        );
        this.deletingProduct.set(null);
        this.isDeleting = false;
        this.toastService.show('Product deleted successfully', 'success', 3500);
      },
      error: (err) => {
        this.toastService.show(err.error?.message || 'Failed to delete product', 'error', 3500);
        this.deletingProduct.set(null);
        this.isDeleting = false;
      },
    });
  }
  cancelDelete(): void {
    this.deletingProduct.set(null);
  }
}
