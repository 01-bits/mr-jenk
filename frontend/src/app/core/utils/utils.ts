export function ValidImage(type: string) {
  const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];
  return IMAGE_TYPES.includes(type);
}


export function VerifySize( size: number) {
  const convertedSize = parseFloat((size / 1000000).toFixed(3))

  return convertedSize < 2;
}
