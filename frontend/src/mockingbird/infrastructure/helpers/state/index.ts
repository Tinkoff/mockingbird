export function selectorAsIs(state: any) {
  return state;
}

export function extractError(e: any) {
  if (!e || !e.body) return;
  if (e.body.error) {
    if (e.body.messages) return [e.body.error, ...e.body.messages].join('. ');
    return e.body.error;
  }
  return e.body;
}
