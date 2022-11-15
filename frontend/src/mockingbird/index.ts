import createApp, { renderSlots } from 'src/infrastructure/helpers/app';
import { Layout } from './layers/layout/Layout';
import Header from './layers/layout/Header';
import paths from './paths';
import { configureSettings } from './settings';
import './main.css';

createApp({
  name: 'mockingbird',
  modulesMap: {
    errorInterceptor: true,
    clientHints: true,
  },
  routes: routesFactory,
  render: () => renderSlots({}),
  envs: [
    'NODE_ENV',
    'ASSETS_PREFIX',
    'RELATIVE_PATH',
    'MOCKINGBIRD_API',
    'MOCKINGBIRD_EXEC_API',
  ],
  meta: {
    title:
      'Mockingbird - сервис эмуляции REST-сервисов и сервисов с интерфейсами-очередями',
  },
  layout: Layout,
  header: Header,
  bundles: {
    // регистрируем бандл, который будет использоваться для всех страниц по умолчанию
    mainDefault: () =>
      import(/* webpackChunkName: "mainDefault" */ './bundles/mainDefault'),
  },
});

function routesFactory({ environmentManager }: { environmentManager: any }) {
  const relativePath = environmentManager.get('RELATIVE_PATH');
  configureSettings({ relativePath });
  return [
    {
      name: 'services',
      path: `${relativePath}${paths.services}`,
      config: {
        pageComponent: 'pageServices',
      },
    },
    {
      name: 'sources',
      path: `${relativePath}${paths.sources}`,
      config: {
        pageComponent: 'pageSources',
      },
    },
    {
      name: 'source',
      path: `${relativePath}${paths.source}`,
      config: {
        pageComponent: 'pageSource',
      },
    },
    {
      name: 'sourceNew',
      path: `${relativePath}${paths.sourceNew}`,
      config: {
        pageComponent: 'pageSourceNew',
      },
    },
    {
      name: 'destinations',
      path: `${relativePath}${paths.destinations}`,
      config: {
        pageComponent: 'pageDestinations',
      },
    },
    {
      name: 'destination',
      path: `${relativePath}${paths.destination}`,
      config: {
        pageComponent: 'pageDestination',
      },
    },
    {
      name: 'destinationNew',
      path: `${relativePath}${paths.destinationNew}`,
      config: {
        pageComponent: 'pageDestinationNew',
      },
    },
    {
      name: 'mocks',
      path: `${relativePath}${paths.mocks}`,
      config: {
        pageComponent: 'pageMocks',
      },
    },
    {
      name: 'mock',
      path: `${relativePath}${paths.mock}`,
      config: {
        pageComponent: 'pageMock',
      },
    },
    {
      name: 'mockNew',
      path: `${relativePath}${paths.mockNew}`,
      config: {
        pageComponent: 'pageMockNew',
      },
    },

    {
      name: 'notfound',
      path: `${relativePath}/404`,
      config: {
        pageComponent: 'pageNotfound',
      },
    },
    {
      name: 'notfound',
      path: '*',
      config: {
        pageComponent: 'pageNotfound',
      },
    },

    // для разработки
    {
      name: 'redirect',
      path: '/',
      redirect: `${relativePath}`,
    },
  ];
}
