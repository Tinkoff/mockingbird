import settings from './settings';

const paths = {
  services: '/',
  sources: '/sources',
  source: '/source',
  sourceNew: '/source/new',
  destinations: '/destinations',
  destination: '/destination',
  destinationNew: '/destination/new',
  mocks: '/mocks',
  mock: '/mock',
  mockNew: '/mock/new',
};

export default paths;

export function getPathServices() {
  return `${settings.relativePath}${paths.services}`;
}

export function getPathSources(service: string) {
  return `${settings.relativePath}${paths.sources}?service=${encode(service)}`;
}

export function getPathSource(service: string, source: string) {
  return `${settings.relativePath}${paths.source}?service=${encode(
    service
  )}&source=${encode(source)}`;
}

export function getPathSourceNew(service: string) {
  return `${settings.relativePath}${paths.sourceNew}?service=${encode(
    service
  )}`;
}

export function getPathDestinations(service: string) {
  return `${settings.relativePath}${paths.destinations}?service=${encode(
    service
  )}`;
}

export function getPathDestination(service: string, destination: string) {
  return `${settings.relativePath}${paths.destination}?service=${encode(
    service
  )}&destination=${encode(destination)}`;
}

export function getPathDestinationNew(service: string) {
  return `${settings.relativePath}${paths.destinationNew}?service=${encode(
    service
  )}`;
}

export function getPathMocks(service: string) {
  return `${settings.relativePath}${paths.mocks}?service=${encode(service)}`;
}

export function getPathMockNew(service: string) {
  return `${settings.relativePath}${paths.mockNew}?service=${encode(service)}`;
}

export function getPathMock(service: string, mock: string, type: string) {
  return `${settings.relativePath}${paths.mock}?service=${encode(
    service
  )}&mock=${encode(mock)}&type=${encode(type)}`;
}

function encode(v: string) {
  return encodeURIComponent(v);
}
