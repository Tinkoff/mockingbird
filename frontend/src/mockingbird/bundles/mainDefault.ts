import { createBundle } from '@tramvai/core';
import Services from '../modules/services';
import { PageSources } from '../modules/sources';
import { PageSource, PageSourceNew } from '../modules/source';
import { PageDestinations } from '../modules/destinations';
import { PageDestination, PageDestinationNew } from '../modules/destination';
import Mocks from '../layers/pages/Mocks';
import Mock from '../layers/pages/Mock';
import MockNew from '../layers/pages/MockNew';
import NotFound from '../layers/pages/NotFound';

export default createBundle({
  name: 'mainDefault',
  components: {
    // регистрируем компонент страницы, который будет использоваться для всех страниц, к которым привязан этот бандл, по умолчанию
    pageDefault: Services,

    // регистрируем компонент страницы, который будет использован при соответствующих настройках роута
    pageServices: Services,
    pageSources: PageSources,
    pageSource: PageSource,
    pageSourceNew: PageSourceNew,
    pageDestinations: PageDestinations,
    pageDestination: PageDestination,
    pageDestinationNew: PageDestinationNew,
    pageMocks: Mocks,
    pageMock: Mock,
    pageMockNew: MockNew,

    // страница не найдена
    pageNotfound: NotFound,
  },
  // регистрируем экшены, которые будут выполняться для всех страниц, к которым привязан этот бандл
  actions: [],
});
