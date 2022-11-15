module.exports = {
  extends: ['@tinkoff/stylelint-config/less'],
  ignoreFiles: ['**/dist/**', '**/node_modules/**'],
  rules: {
    'selector-class-pattern': null,
    'property-no-unknown': [
      true,
      {
        ignoreProperties: ['composes'],
      },
    ],
  },
};
