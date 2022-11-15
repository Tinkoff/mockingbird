import LRU from 'lru-cache';

const allCaches = [];

const getOpts = (opts) => {
  const defaults = { max: 1000 };
  return { ...defaults, ...opts };
};

const lruCache = (opts) => {
  const cache = new LRU(getOpts(opts));

  allCaches.push(cache);

  return cache;
};

export const clearAllCaches = () => {
  allCaches.forEach((cache) => cache.reset());
};

export default lruCache;
