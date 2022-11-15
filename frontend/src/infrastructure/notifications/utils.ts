import {
  successToast,
  errorToast,
} from 'src/mockingbird/infrastructure/helpers/toast';
import { extractError } from 'src/mockingbird/infrastructure/helpers/state';
import { addToast } from './store/store';

export function getSuccessToast(title: string) {
  return addToast(
    successToast({
      title,
    })
  );
}

export function getCreateErrorToast(e: any) {
  return getErrorToast('Произошла ошибка при создании', e);
}

export function getUpdateErrorToast(e: any) {
  return getErrorToast('Произошла ошибка при обновлении', e);
}

export function getRemoveErrorToast(e: any) {
  return getErrorToast('Произошла ошибка при удалении', e);
}

function getErrorToast(title: string, e: any) {
  return addToast(
    errorToast({
      title,
      children: e ? extractError(e) : undefined,
    })
  );
}
