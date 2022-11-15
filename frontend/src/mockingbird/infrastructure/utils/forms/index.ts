export function parseJSON(json: string, nullByEmpty?: boolean) {
  let result = null;
  if (!json) return result;
  try {
    result = JSON.parse(json);
    if (nullByEmpty && !Object.keys(result).length) return null;
  } catch (e) {}
  return result;
}

export function stringifyJSON(data: any, defaultValue = {}) {
  return JSON.stringify(data || defaultValue, undefined, 2);
}
