const SERVICE = 'blePeripheralPlugin';

function initialize() {
  return new Promise((resolve, reject) => {
    cordova.exec(resolve, reject, SERVICE, 'initialize', []);
  });
}

function getAdapterInfo() {
  return new Promise((resolve, reject) => {
    cordova.exec(resolve, reject, SERVICE, 'getAdapterInfo', []);
  });
}

function requestEnable() {
  return new Promise((resolve, reject) => {
    cordova.exec(resolve, reject, SERVICE, 'requestEnable', []);
  });
}

function startAdvertising(_manufacturerId, _data) {
  return new Promise((resolve, reject) => {
    cordova.exec(resolve, reject, SERVICE, 'startAdvertising', [
      _manufacturerId,
      _data
    ]);
  });
}

function stopAdvertising() {
  return new Promise((resolve, reject) => {
    cordova.exec(resolve, reject, SERVICE, 'stopAdvertising', []);
  });
}

module.exports = {
  initialize,
  getAdapterInfo,
  requestEnable,
  startAdvertising,
  stopAdvertising
};
