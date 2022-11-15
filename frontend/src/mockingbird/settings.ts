type Settings = {
  relativePath: string;
};

const settings: Settings = {
  relativePath: '',
};

export function configureSettings(configuredSettings: Settings) {
  Object.assign(settings, configuredSettings);
}

export default settings;
