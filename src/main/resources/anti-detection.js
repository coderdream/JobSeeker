Object.defineProperty(navigator, 'webdriver', {
  get: function () {
    return undefined;
  }
});

Object.defineProperty(navigator, 'languages', {
  get: function () {
    return ['zh-CN', 'zh'];
  }
});

Object.defineProperty(navigator, 'plugins', {
  get: function () {
    return [1, 2, 3, 4, 5];
  }
});

Object.defineProperty(navigator, 'platform', {
  get: function () {
    return 'MacIntel';
  }
});

if (!window.chrome) {
  window.chrome = {};
}
if (!window.chrome.runtime) {
  window.chrome.runtime = {};
}

delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;
delete window.cdc_adoQpoasnfa76pfcZLmcfl_JSON;
delete window.cdc_adoQpoasnfa76pfcZLmcfl_Object;
delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;
delete window.cdc_adoQpoasnfa76pfcZLmcfl_Proxy;
delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;
delete window.cdc_adoQpoasnfa76pfcZLmcfl_Window;

var originalQuery = window.navigator.permissions && window.navigator.permissions.query;
if (originalQuery) {
  window.navigator.permissions.query = function (parameters) {
    if (parameters && parameters.name === 'notifications') {
      return Promise.resolve({ state: Notification.permission });
    }
    return originalQuery.call(window.navigator.permissions, parameters);
  };
}
