import { Component, inject, OnDestroy, signal } from '@angular/core';
import { UserService } from '../../../../core/services/user-service/user.service';
import { CommonModule } from '@angular/common';
import { MediaService } from '../../../../core/services/media-service/media.service';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ValidImage, VerifySize } from '../../../../core/utils/utils';
import { API_URL } from '../../../../core/const/variables';
import { DashboardPage } from '../dashboard-page/dashboard-page';
import { ToastService } from '../../../../core/services/toast-service/toast.service';

@Component({
  imports: [CommonModule, ReactiveFormsModule, DashboardPage],
  selector: 'app-profile-page',
  styleUrl: './profile-page.css',
  templateUrl: './profile-page.html',
})
export class ProfilePage implements OnDestroy {
  ngOnDestroy(): void {
    this.mediaService.removeFiles();

  }
  userService = inject(UserService)
  newAvatarUrl = signal<string | undefined | null>(this.userService.currentUser()?.avatar);
  mediaService = inject(MediaService)
  toastService = inject(ToastService)

  createForm = new FormGroup({
    media: new FormControl()
  })


  get media() {
    return this.createForm.get('media');
  }

  getAvatarUrl(): string {
    const avatar = this.userService.currentUser()?.avatar;

    if (avatar) {
      return `${API_URL}/media/images/${avatar}`;
    }

    return 'images/AvatarMaker.png';
  }

  onUpdateAvatar(event: Event) {
    const inputElement = event.target as HTMLInputElement;
    if (!inputElement.files || inputElement.files.length === 0) {
      return;
    }
    const file = inputElement.files[0];

    if (((!ValidImage(file.type) || !VerifySize(file.size)) && file.type.startsWith("image/"))) {
      this.createForm.controls.media.markAllAsTouched();
      this.createForm.controls.media.setErrors({
        invalidMedia: true,
      });
      return;
    }

    if (this.createForm.invalid) {
      this.createForm.markAsUntouched();
    }

    const file_url = this.mediaService.serveMediaLocaly(file);

    this.newAvatarUrl.set(file_url)


  }

  onDeleteNewAvatar() {

    this.mediaService.removeFiles()
    this.newAvatarUrl.set(null)
    if (this.createForm.invalid) {
      this.createForm.markAsUntouched();
    }
  }

  oneSubmit() {
    const form = new FormData();
    form.append("file", this.mediaService.mediaItems()[0].file)
    this.mediaService.UploadMedia(form).subscribe({
      next: res => {
        this.userService.currentUser.update(prev => prev ? {
          ...prev,
          avatar: res.MediaId
        } : null);

        this.newAvatarUrl.set(null)
        this.mediaService.removeFiles();

      },
      error: err => {
        const errorMessage = err.error?.message || 'An error occurred while uploading the avatar.';
        this.toastService.show(errorMessage, "error", 3500);
      }
    })
  }


}
