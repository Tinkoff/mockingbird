import type { ComponentType } from 'react';
import type { ModuleType, ExtendedModule } from '@tramvai/core';
import { createApp as createTramvaiApp } from '@tramvai/core';
import type { EnvironmentManager } from '@tramvai/module-common';
import {
  CommonModule,
  ENV_MANAGER_TOKEN,
  ENV_USED_TOKEN,
} from '@tramvai/module-common';
import { LogModule } from '@tramvai/module-log';
import { SpaRouterModule, ROUTES_TOKEN } from '@tramvai/module-router';
import {
  RENDER_SLOTS,
  RenderModule,
  ResourceSlot,
  ResourceType,
} from '@tramvai/module-render';
import {
  META_DEFAULT_TOKEN,
  META_UPDATER_TOKEN,
  SeoModule,
} from '@tramvai/module-seo';
import { ServerModule } from '@tramvai/module-server';
import { ErrorInterceptorModule } from '@tramvai/module-error-interceptor';
import { ClientHintsModule } from '@tramvai/module-client-hints';
import { SentryModule } from '@tramvai/module-sentry';
import type { PageResource } from '@tramvai/tokens-render';
import {
  DEFAULT_HEADER_COMPONENT,
  LAYOUT_OPTIONS,
  TRAMVAI_RENDER_MODE,
} from '@tramvai/tokens-render';
import type { PageService } from '@tramvai/tokens-router';
import { PAGE_SERVICE_TOKEN } from '@tramvai/tokens-router';
import type { ServerModuleStaticsOptions } from '@tramvai/tokens-server';
import { SERVER_MODULE_STATICS_OPTIONS } from '@tramvai/tokens-server';
import type { Route } from '@tinkoff/router';
import {
  PageRenderModeModule,
  PAGE_RENDER_DEFAULT_FALLBACK_COMPONENT,
  PAGE_RENDER_WRAPPER_TYPE,
} from '@tramvai/module-page-render-mode';

interface ModulesMap {
  sentry?: boolean;
  errorInterceptor?: boolean;
  clientHints?: boolean;
}

type Modules = (ModuleType | ExtendedModule)[];
type Routes =
  | Route[]
  | ((opt: { environmentManager: EnvironmentManager }) => Route[]);

interface Config {
  // Имя приложения
  name: string;
  // Роуты приложения (список роутов или фунция, возвращающая их)
  routes: Routes;
  // Функция, возвращающая ресурсы (скрипты, стили, иконки), которые нужно отрендерить
  render: (opt: { environmentManager: EnvironmentManager }) => PageResource[];
  // Мета-теги для SEO
  meta: Record<string, string> | ((opt: { pageService: PageService }) => void);
  // Список токенов которые необходимы модулю или приложению.
  // Позднее они будут доступны через `environmentManager`
  envs: string[];
  // Объект с подключаемыми бандлами с данными в приложении.
  // Ключ - индетификатор бандла, значение - Promise, который возвращает bundle
  // Подробнее в @tramvai/core/lib/createApp.d.ts
  bundles: any;
  // Массив с глобальными экшенами, которые будут зарегистрированы для всех бандлов и страниц
  // Подробнее в @tramvai/core/lib/createApp.d.ts
  actions?: any;
  // Дает возможность подключать некоторые общие модули опционально (см. getModules ниже)
  modulesMap?: ModulesMap;
  // Список модулей (обычно кастомизированных), которые нужно подключить
  // и которых нет в общем списке (см. getModules ниже)
  customModules?: Modules;
  // layout, который будет использоваться для всех страниц по умолчанию
  layout?: ComponentType<any>;
  // header, который будет использоваться для всех страниц по умолчанию
  header?: ComponentType<any>;
  // Настройки для работы статики
  serverStaticsOptions?: ServerModuleStaticsOptions;
}

export default function createApp(config: Config) {
  const { name, bundles, actions } = config;
  return createTramvaiApp({
    name,
    modules: getModules(config),
    bundles,
    actions,
    providers: getProviders(config),
  });
}

function getModules(config: Config) {
  const { modulesMap, customModules, routes } = config;
  const modules = [
    modulesMap?.sentry && SentryModule,
    CommonModule,
    LogModule,
    typeof routes === 'function'
      ? SpaRouterModule
      : SpaRouterModule.forRoot(routes),
    RenderModule.forRoot({ mode: 'strict' }),
    SeoModule,
    ServerModule,
    PageRenderModeModule,
    modulesMap?.errorInterceptor && ErrorInterceptorModule,
    modulesMap?.clientHints && ClientHintsModule,
  ].filter(Boolean);
  return customModules && customModules.length
    ? [...modules, ...customModules]
    : modules;
}

function getProviders(config: Config) {
  const { routes, render, meta, envs, layout, header, serverStaticsOptions } =
    config;
  const result = [
    {
      provide: RENDER_SLOTS,
      multi: true,
      useFactory: render,
      deps: {
        environmentManager: 'environmentManager',
      },
    },
    {
      provide: ENV_USED_TOKEN,
      useValue: envs.map((key) => ({
        key,
        dehydrate: true,
      })),
      multi: true,
    },
    {
      provide: PAGE_RENDER_WRAPPER_TYPE,
      useValue: 'layout',
    },
    {
      provide: PAGE_RENDER_DEFAULT_FALLBACK_COMPONENT,
      useValue: () => null,
    },
    {
      provide: TRAMVAI_RENDER_MODE,
      useValue: 'client',
    },
  ];
  if (typeof routes === 'function')
    result.push({
      provide: ROUTES_TOKEN,
      multi: true,
      useFactory: routes,
      deps: {
        environmentManager: ENV_MANAGER_TOKEN,
      },
    });
  if (meta) {
    if (typeof meta === 'function') {
      result.push({
        provide: META_UPDATER_TOKEN,
        multi: true,
        useFactory: meta,
        deps: {
          pageService: PAGE_SERVICE_TOKEN,
        },
      });
    } else {
      result.push({
        provide: META_DEFAULT_TOKEN,
        useValue: meta,
      });
    }
  }
  if (layout)
    result.push({
      provide: LAYOUT_OPTIONS,
      multi: true,
      useValue: {
        components: {
          content: layout,
        },
      },
    });
  if (header)
    result.push({
      provide: DEFAULT_HEADER_COMPONENT,
      useValue: header,
    });
  if (serverStaticsOptions)
    result.push({
      provide: SERVER_MODULE_STATICS_OPTIONS,
      useValue: serverStaticsOptions,
    });
  return result;
}

export function renderSlots({
  icons,
  appleIcons,
  headStyles,
  headScripts,
}: {
  icons?: { [key: number]: string };
  appleIcons?: { [key: number]: string };
  headStyles?: string[];
  headScripts?: string[];
}) {
  const slots = [];
  if (icons)
    Object.keys(icons).forEach((size) =>
      slots.push({
        type: ResourceType.iconLink,
        slot: ResourceSlot.HEAD_ICONS,
        payload: icons[size],
        attrs: {
          rel: 'icon',
          sizes: `${size}x${size}`,
        },
      })
    );
  if (appleIcons)
    Object.keys(appleIcons).forEach((size) =>
      slots.push({
        type: ResourceType.iconLink,
        slot: ResourceSlot.HEAD_ICONS,
        payload: appleIcons[size],
        attrs: {
          rel: 'apple-touch-icon',
          sizes: `${size}x${size}`,
        },
      })
    );
  if (headStyles)
    headStyles.forEach((payload) =>
      slots.push({
        type: ResourceType.asIs,
        slot: ResourceSlot.HEAD_CORE_STYLES,
        payload,
      })
    );
  if (headScripts)
    headScripts.forEach((payload) =>
      slots.push({
        type: ResourceType.asIs,
        slot: ResourceSlot.HEAD_CORE_SCRIPTS,
        payload,
      })
    );
  return slots;
}

export function getYandexMetrikaScript(id: number) {
  return `
<script type="text/javascript">
  (function(m,e,t,r,i,k,a){m[i]=m[i]||function(){(m[i].a=m[i].a||[]).push(arguments)};
  m[i].l=1*new Date();k=e.createElement(t),a=e.getElementsByTagName(t)[0],k.async=1,k.src=r,a.parentNode.insertBefore(k,a)})
  (window, document, "script", "https://mc.yandex.ru/metrika/tag.js", "ym");
  ym(${id}, "init", {
    clickmap: true,
    trackLinks: true,
    accurateTrackBounce: true,
    webvisor: true
  });
</script>
<noscript><div><img src="https://mc.yandex.ru/watch/${id}" style="position:absolute; left:-9999px;" alt="" /></div></noscript>
  `;
}
