import { inject, Service, signal } from '@angular/core';
import { MediaData } from '../../shared/interfaces/Media';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../const/variables';


interface MediaResponse{
  MediaId: string
}
@Service()
export class MediaService {

  mediaItems = signal<MediaData[]>([])
  http=inject(HttpClient)


  serveMediaLocaly(file: File): string | null {
    if (this.mediaItems.length > 4) {
      return null;
    }



    const item: MediaData = {
      file,
      previewUrl: URL.createObjectURL(file),
    };

    this.mediaItems().push(item);


    return item.previewUrl;
  }

  removeFiles() {
    this.mediaItems.set([])
  }

  UploadMedia(file: FormData){
    return this.http.put<MediaResponse>(`${API_URL}/profile/me`,file)
  }
}
