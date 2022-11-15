export function scrollTop(behavior: 'smooth' | 'auto' = 'auto') {
  window.scrollTo({ top: 0, behavior });
}

export function scrollToId(id: string, behavior: 'smooth' | 'auto' = 'smooth') {
  const el = window.document.getElementById(id);
  if (el) el.scrollIntoView({ behavior });
}
