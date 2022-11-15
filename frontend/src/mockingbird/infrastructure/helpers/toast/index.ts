export function successToast(item: any) {
  return {
    type: 'success',
    timer: 3000,
    ...item,
  };
}

export function errorToast(item: any) {
  return {
    type: 'error',
    timer: 5000,
    ...item,
  };
}
