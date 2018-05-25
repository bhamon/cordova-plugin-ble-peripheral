package com.cryo.airlock;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Plugin extends CordovaPlugin {
  private final static String ACTION_INITIALIZE = "initialize";
  private final static String ACTION_GET_ADAPTER_INFO = "getAdapterInfo";
  private final static String ACTION_REQUEST_ENABLE = "requestEnable";
  private final static String ACTION_START_ADVERTISING = "startAdvertising";
  private final static String ACTION_STOP_ADVERTISING = "stopAdvertising";

  private final static String ERROR_VALIDATION = "validation";
  private final static String ERROR_UNAVAILABLE = "unavailable";
  private final static String ERROR_NOT_INITIALIZED = "notInitialized";
  private final static String ERROR_DISABLED = "disabled";
  private final static String ERROR_ALREADY_ADVERTISING = "alreadyAdvertising";
  private final static String ERROR_NOT_ADVERTISING = "notAdvertising";
  private final static String ERROR_START_ADVERTISING = "startAdvertising";

  private final static String JSON_KEY_ERROR_CODE = "code";
  private final static String JSON_KEY_ERROR_DETAILS = "details";

  private final static int REQUEST_ENABLE = 51561;
  private final static int PACKET_MAX_LENGTH = 27;

  private BluetoothAdapter m_bluetoothAdapter = null;
  private AdvertiseCallback m_advertiseCallback = null;
  private CallbackContext m_requestEnableCallbackContext = null;
  private CallbackContext m_advertiseCallbackContext = null;
  private boolean m_advertising = false;

  public Plugin() {
  }

  @Override
  public boolean execute(
    String p_action,
    final JSONArray p_args,
    final CallbackContext p_callback
  ) throws JSONException {
    switch(p_action) {
      case ACTION_INITIALIZE:
        initialize(p_callback);
        break;
      case ACTION_GET_ADAPTER_INFO:
        getAdapterInfo(p_callback);
        break;
      case ACTION_REQUEST_ENABLE:
        requestEnable(p_callback);
        break;
      case ACTION_START_ADVERTISING:
        startAdvertising(p_args.getInt(0), p_args.getJSONArray(1), p_callback);
        break;
      case ACTION_STOP_ADVERTISING:
        stopAdvertising(p_callback);
        break;
    }
  }

  @Override
  public void onActivityResult(int p_requestCode, int p_resultCode, Intent p_intent) {
    switch(p_requestCode) {
      case REQUEST_ENABLE:
        if(p_resultCode == Activity.RESULT_OK) {
          m_requestEnableCallbackContext.success(true);
        } else {
          JSONObject ret = new JSONObject();
          ret.put(JSON_KEY_ERROR_CODE, ERROR_REJECTED);
          m_requestEnableCallbackContext.error(ret);
        }

        m_requestEnableCallbackContext = null;
    }
  }

  private void initialize(CallbackContext p_callback) {
    BluetoothManager manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
    m_bluetoothAdapter = manager.getAdapter();
    if(m_bluetoothAdapter == null) {
      JSONObject ret = new JSONObject();
      ret.put(JSON_KEY_ERROR_CODE, ERROR_UNAVAILABLE);
      p_callback.error(ret);
      return;
    }

    m_advertiseCallback = new AdvertiseCallback() {
      @Override
      public void onStartSuccess(AdvertiseSettings p_settings) {
        m_advertising = true;

        if(m_advertiseCallbackContext == null) {
          return;
        }

        JSONObject ret = new JSONObject();
        ret.put("mode", p_settings.getMode());
        ret.put("timeout", p_settings.getTimeout());
        ret.put("txPowerLevel", p_settings.getTxPowerLevel());
        ret.put("connectable", p_settings.isConnectable());

        m_advertiseCallbackContext.success(ret);
        m_advertiseCallbackContext = null;
      }

      @Override
      public void onStartFailure(int p_errorCode) {
        m_advertising = false;

        if(m_advertiseCallbackContext == null) {
          return;
        }

        JSONObject ret = new JSONObject();
        JSONObject details = new JSONObject();
        ret.put(JSON_KEY_ERROR_CODE, ERROR_START_ADVERTISING);
        ret.put(JSON_KEY_ERROR_DETAILS, details);

        switch(p_errorCode) {
          case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
            details.put("reason", "alreadyStarted");
            break;
          case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
            details.put("reason", "dataTooLarge");
            break;
          case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
            details.put("reason", "featureUnsupported");
            break;
          case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
            details.put("reason", "intervalError");
            break;
          case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
            details.put("reason", "tooManyAdvertisers");
            break;
          default:
            details.put("reason", p_errorCode);
        }

        m_advertiseCallbackContext.error(ret);
        m_advertiseCallbackContext = null;
      }
    };

    p_callback.success();
  }

  private boolean checkAdapter(CallbackContext p_callback) {
    if(m_bluetoothAdapter != null) {
      return true;
    }

    JSONObject ret = new JSONObject();
    ret.put(JSON_KEY_ERROR_CODE, ERROR_NOT_INITIALIZED);
    p_callback.error(ret);
    return false;
  }

  private boolean checkEnabled(CallbackContext p_callback) {
    if(m_bluetoothAdapter != null && m_bluetoothAdapter.isEnabled()) {
      return true;
    }

    JSONObject ret = new JSONObject();
    ret.put(JSON_KEY_ERROR_CODE, ERROR_DISABLED);
    p_callback.error(ret);
    return false;
  }

  private void getAdapterInfo(CallbackContext p_callback) {
    if(!checkAdapter(p_callback)) {
      return;
    }

    JSONObject ret = new JSONObject();
    ret.put("address", m_bluetoothAdapter.getAddress());
    ret.put("name", m_bluetoothAdapter.getName());
    ret.put("enabled", m_bluetoothAdapter.isEnabled());
    ret.put("advertising", m_advertising);

    p_callback.success(ret);
  }

  private void requestEnable(CallbackContext p_callback) {
    if(!checkAdapter(p_callback)) {
      return;
    } else if(m_bluetoothAdapter.isEnabled()) {
      p_callback.success(false);
      return;
    }

    m_requestEnableCallbackContext = m_callback;

    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    cordova.startActivityForResult(this, intent, REQUEST_ENABLE);
  }

  private void startAdvertising(int p_manufacturerId, JSONArray p_data, CallbackContext p_callback) {
    if(!checkAdapter(p_callback) || !checkEnabled(p_callback)) {
      return;
    } else if(m_advertising) {
      JSONObject ret = new JSONObject();
      JSONObject details = new JSONObject();
      ret.put(JSON_KEY_ERROR_CODE, ERROR_ALREADY_ADVERTISING);
      p_callback.error(ret);
    } else if(p_manufacturerId < 0 || p_manufacturerId > 0x0ffff) {
      JSONObject ret = new JSONObject();
      JSONObject details = new JSONObject();
      ret.put(JSON_KEY_ERROR_CODE, ERROR_VALIDATION);
      res.put(JSON_KEY_ERROR_DETAILS, details);
      details.put("argument", "manufacturerId");
      p_callback.error(ret);
      return;
    } else if(p_data == null || p_data.length() > PACKET_MAX_LENGTH) {
      JSONObject ret = new JSONObject();
      JSONObject details = new JSONObject();
      ret.put(JSON_KEY_ERROR_CODE, ERROR_VALIDATION);
      res.put(JSON_KEY_ERROR_DETAILS, details);
      details.put("argument", "data");
      p_callback.error(ret);
      return;
    }

    BluetoothLeAdvertiser advertiser = m_bluetoothAdapter.getBluetoothLeAdvertiser();
    AdvertiseSettings settings = new AdvertiseSettings.Builder()
      .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
      .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
      .setConnectable(false)
      .build();

    AdvertiseData data = new AdvertiseData.Builder()
      .setIncludeDeviceName(false)
      .setIncludeTxPowerLevel(false)
      .addManufacturerData(p_manufacturerId, p_data)
      .build();

    m_advertiseCallbackContext = p_callback;
    advertiser.startAdvertising(settings, data, m_advertiseCallback);
  }

  private void stopAdvertising(CallbackContext p_callback) {
    if(!checkAdapter(p_callback) || !checkEnabled(p_callback)) {
      return;
    } else if(!m_advertising) {
      JSONObject ret = new JSONObject();
      JSONObject details = new JSONObject();
      ret.put(JSON_KEY_ERROR_CODE, ERROR_NOT_ADVERTISING);
      p_callback.error(ret);
    }

    BluetoothLeAdvertiser advertiser = m_bluetoothAdapter.getBluetoothLeAdvertiser();
    advertiser.stopAdvertising(callback);

    m_advertising = false;
  }
}
