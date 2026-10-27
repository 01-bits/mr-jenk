import { inject } from '@angular/core'
import { CanActivateFn, Router } from '@angular/router'
import { UserService } from '../../services/user-service/user.service'
import { TOKEN_KEY } from '../../const/variables'

export const guestGuard: CanActivateFn = (route, state) => {

  const userService = inject(UserService)
  const router = inject(Router)


  if (userService.getlocalStorage(TOKEN_KEY)==='' || !userService.currentUser()){
    return true
  }

  router.navigateByUrl('')
  return false
}
