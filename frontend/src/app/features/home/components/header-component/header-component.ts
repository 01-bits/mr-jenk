import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { UserService } from '../../../../core/services/user-service/user.service';
import { Router, RouterLink } from '@angular/router';
import { API_URL, TOKEN_KEY } from '../../../../core/const/variables';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Filevalidator } from '../../../../core/utils/getRealMimeType';
import { ProductService } from '../../../../core/services/product-service/product.service';
import { FilePreview } from '../../../../core/shared/interfaces/Media';
import { ToastService } from '../../../../core/services/toast-service/toast.service';
import { ValidImage } from '../../../../core/utils/utils';
import { catchError, finalize, map, of, switchMap, throwError } from 'rxjs';
import { ThemeService } from '../../../../core/services/theme-servicee/theme.service';

@Component({
  imports: [CommonModule,  RouterLink, ReactiveFormsModule],
  selector: 'app-header-component',
  styleUrl: './header-component.css',
  templateUrl: './header-component.html',
})
export class HeaderComponent {
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private productService = inject(ProductService);
  private getRealMimeType = inject(Filevalidator);
  private notificationToast = inject(ToastService);
  userService = inject(UserService);
  isMenuOpen = signal<boolean>(false);
  ToggleCreatePostOpen = signal<boolean>(false);
  previews = signal<FilePreview[]>([]);
  user = this.userService.currentUser;
  createProductForm: FormGroup;
  selectedFiles: File[] = [];
  isSubmitting = false;
  readonly MAX_FILE_SIZE = 2 * 1024 * 1024;
  readonly MAX_FILES = 5;

  constructor() {
    this.createProductForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(2000)]],
      price: [null, [Validators.required, Validators.min(0.01), Validators.max(999999999)]],
      quantity: [0, [Validators.required, Validators.min(0), Validators.max(2147483647)]],
    });
  }

  themeService = inject(ThemeService);

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  toggleMenu() {
    this.isMenuOpen.update((isOpen) => !isOpen);
  }

  closeMenu() {
    this.isMenuOpen.set(false);
  }

  getAvatarUrl(): string {
    const avatar = this.userService.currentUser()?.avatar;

    if (avatar) {
      return `${API_URL}/media/images/${avatar}`;
    }

    return 'images/AvatarMaker.png';
  }


  isActive(path: string): boolean {
    return this.router.url === path;
  }

  logout() {
    this.userService.setlocalStorage(TOKEN_KEY, null);
    this.userService.SetCurrentUser(null);
  }

  async onFileSelected(event: any): Promise<void> {
    if (!event.target.files || event.target.files.length === 0) {
      return;
    }
    if (event.target.files.length > this.MAX_FILES) {
      this.notificationToast.show(
        `You can only upload up to ${this.MAX_FILES} files.`,
        'error',
        3500,
      );
      return;
    }
    const files = Array.from(event.target.files) as File[];
    this.clearPreviews();

    for (const file of files) {
      if (file.size > this.MAX_FILE_SIZE) {
        this.notificationToast.show(
          `File ${file.name} exceeds the maximum size of 2MB.`,
          'error',
          3500,
        );
        continue;
      }
      try {
        const realMimeType = await this.getRealMimeType.validateRealMimeType(file);
        if (!ValidImage(realMimeType)) {
          this.notificationToast.show(`File ${file.name} has an invalid format.`, 'error', 3500);
          continue;
        }

        this.selectedFiles.push(file);
        const previewUrl = URL.createObjectURL(file);

        this.previews.update((prev) => [
          ...prev,
          {
            url: previewUrl,
            name: file.name,
          },
        ]);
      } catch (error) {
        this.notificationToast.show(`Error validating file: ${file.name}`, 'error', 3500);
      }
    }
  }

  removeFile(index: number): void {
    URL.revokeObjectURL(this.previews()[index].url);
    this.selectedFiles.splice(index, 1);

    this.previews.update((prev) => prev.filter((_, i) => i !== index));
  }

  private clearPreviews(): void {
    this.previews().forEach((p) => URL.revokeObjectURL(p.url));
    this.previews.set([]);
    this.selectedFiles = [];
  }

  toggleCreateProduct(): void {
    this.ToggleCreatePostOpen.set(true);
  }

  closeCreateProductModal(): void {
    this.ToggleCreatePostOpen.set(false);
    this.clearPreviews();
    this.createProductForm.reset({ name: '', description: '', price: null, quantity: 0 });
  }

  onSubmit(): void {
    if (this.isSubmitting) return;
    if (this.createProductForm.invalid) {
      this.createProductForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formValues = this.createProductForm.getRawValue();

    const productPayload = {
      name: formValues.name,
      description: formValues.description || null,
      price: Number(formValues.price),
      quantity: Number(formValues.quantity),
    };

    this.productService
      .createProduct(productPayload)
      .pipe(
        switchMap((createdProduct) => {
          if (this.selectedFiles.length === 0) {
            return of(createdProduct);
          }

          return this.productService.uploadImages(this.selectedFiles, createdProduct.id).pipe(
            map((media) => ({
              ...createdProduct,
              imageUrls: media.map((item) => item.url),
            })),
            catchError((err) => {
              return this.productService.deleteProduct(createdProduct.id).pipe(
                catchError(() => of(void 0)),
                switchMap((err) =>
                  throwError(() => new Error('Image upload failed. Product creation rolled back.')),
                ),
              );
            }),
          );
        }),
        finalize(() => {
          this.clearPreviews();
          this.closeCreateProductModal();
          this.isSubmitting = false;
        }),
      )
      .subscribe({
        next: (finalProduct) => {
          this.closeCreateProductModal();
          this.productService.products.set([finalProduct, ...this.productService.products()]);
          this.productService.allProducts.set([finalProduct, ...this.productService.allProducts()]);
          this.notificationToast.show('Product Created successfully', 'success', 3500);
        },
        error: (err) => {
          this.closeCreateProductModal();
          this.notificationToast.show(err.message || 'Failed to create product', 'error', 3500);
        },
      });
  }
}
