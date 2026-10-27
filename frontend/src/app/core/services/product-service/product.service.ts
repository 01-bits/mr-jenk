import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PagedResponse, ProductResponse } from '../../shared/interfaces/Product';
import { API_URL } from '../../const/variables';

export interface CreateProductRequest {
  name: string;
  description: string | null;
  price: number;
  quantity: number;
}

export interface MediaResponse {
  id: string;
  url: string;
  productId: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  http = inject(HttpClient);
  products = signal<ProductResponse[]>([]);
  allProducts = signal<ProductResponse[]>([]);

  getProducts(
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,desc',
  ): Observable<PagedResponse<ProductResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<PagedResponse<ProductResponse>>(API_URL + '/products', { params });
  }

  getMyProducts(
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,desc',
  ): Observable<PagedResponse<ProductResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<PagedResponse<ProductResponse>>(API_URL + '/products/me', { params });
  }

  createProduct(product: CreateProductRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(API_URL + '/products', product);
  }

  uploadImages(files: File[], productId: string): Observable<MediaResponse[]> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file, file.name));
    formData.append('productId', productId);

    return this.http.post<MediaResponse[]>(API_URL + '/media/images', formData);
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(API_URL + '/products/' + id);
  }

  deleteImage(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/media/images/${encodeURIComponent(id)}`);
  }

  updateProduct(id: string, payload: CreateProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${API_URL}/products/${id}`, payload);
  }
}
