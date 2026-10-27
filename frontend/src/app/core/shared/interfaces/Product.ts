export interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId: string;
  imageUrls: string[];
  createdAt: string;
  updatedAt: string;
  currentMediaIndex: number;
  isMenuOpen: boolean;
}

export interface PageMetadata {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: PageMetadata;
}
