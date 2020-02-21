package by.chemerisuk.cordova.firebase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.util.Base64;
import android.content.Context;
import android.util.Log;

import by.chemerisuk.cordova.support.CordovaMethod;
import by.chemerisuk.cordova.support.ReflectiveCordovaPlugin;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FirebaseConfigPlugin extends ReflectiveCordovaPlugin {
    private static final String TAG = "FirebaseConfigPlugin";

    private FirebaseRemoteConfig firebaseRemoteConfig;

    @Override
    protected void pluginInitialize() {
        Log.d(TAG, "Starting Firebase Remote Config plugin");

        this.firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        String filename = preferences.getString("FirebaseRemoteConfigDefaults", "");
        if (filename.isEmpty()) {
            // always call setDefaults in order to avoid exception
            // https://github.com/firebase/quickstart-android/issues/291
            this.firebaseRemoteConfig.setDefaults(Collections.<String, Object>emptyMap());
        } else {
            Context ctx = cordova.getActivity().getApplicationContext();
            int resourceId = ctx.getResources().getIdentifier(filename, "xml", ctx.getPackageName());
            this.firebaseRemoteConfig.setDefaults(resourceId);
        }
    }

    @CordovaMethod
    protected void fetch(long expirationDuration, final CallbackContext callbackContext) {
        this.firebaseRemoteConfig.fetch(expirationDuration)
                .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            callbackContext.success();
                        } else {
                            callbackContext.error(task.getException().getMessage());
                        }
                    }
                });
    }

    @CordovaMethod
    protected void activate(final CallbackContext callbackContext) {
        this.firebaseRemoteConfig.activate()
                .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            callbackContext.sendPluginResult(
                                    new PluginResult(PluginResult.Status.OK, task.getResult()));
                        } else {
                            callbackContext.error(task.getException().getMessage());
                        }
                    }
                });
    }

    @CordovaMethod
    protected void fetchAndActivate(final CallbackContext callbackContext) {
        this.firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            callbackContext.sendPluginResult(
                                    new PluginResult(PluginResult.Status.OK, task.getResult()));
                        } else {
                            callbackContext.error(task.getException().getMessage());
                        }
                    }
                });
    }

    @CordovaMethod
    protected void getBoolean(String key, CallbackContext callbackContext) {
        callbackContext.sendPluginResult(
                new PluginResult(PluginResult.Status.OK, getValue(key).asBoolean()));
    }

    @CordovaMethod
    protected void getBytes(String key, CallbackContext callbackContext) {
        callbackContext.success(getValue(key).asByteArray());
    }

    @CordovaMethod
    protected void getNumber(String key, CallbackContext callbackContext) {
        callbackContext.sendPluginResult(
                new PluginResult(PluginResult.Status.OK, (float)getValue(key).asDouble()));
    }

    @CordovaMethod
    protected void getString(String key, CallbackContext callbackContext) {
        callbackContext.success(getValue(key).asString());
    }

    @CordovaMethod
    protected void setConfigSettings(final JSONObject config, final CallbackContext callbackContext) {
        long fetchTimeoutInSeconds = config.optLong("fetchTimeoutInSeconds", -1L);
        long minimumFetchIntervalInSeconds = config.optLong("minimumFetchIntervalInSeconds", -1L);

        FirebaseRemoteConfigSettings.Builder settings = new FirebaseRemoteConfigSettings.Builder();
        if(fetchTimeoutInSeconds > 0) {
            settings = settings.setFetchTimeoutInSeconds(fetchTimeoutInSeconds);
        }
        if(minimumFetchIntervalInSeconds > 0) {
            settings = settings.setMinimumFetchIntervalInSeconds(minimumFetchIntervalInSeconds);
        }

        this.firebaseRemoteConfig.setConfigSettings(settings.build());
        callbackContext.success();
    }

    @CordovaMethod
    protected void setDefaults(final JSONObject defaults, final CallbackContext callbackContext) {
        try {
            this.firebaseRemoteConfig.setDefaultsAsync(defaultsToMap(defaults));
            callbackContext.success();
        } catch(Exception e){
            callbackContext.error(e.getMessage());
        }
    }

    private FirebaseRemoteConfigValue getValue(String key) {
        return this.firebaseRemoteConfig.getValue(key);
    }

    private static Map<String, Object> defaultsToMap(JSONObject object) throws JSONException {
        final Map<String, Object> map = new HashMap<String, Object>();

        for (Iterator<String> keys = object.keys(); keys.hasNext(); ) {
            String key = keys.next();
            Object value = object.get(key);

            if (value instanceof Integer) {
                //setDefaults() should take Longs
                value = new Long((Integer) value);
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                if (array.length() == 1 && array.get(0) instanceof String) {
                    //parse byte[] as Base64 String
                    value = Base64.decode(array.getString(0), Base64.DEFAULT);
                } else {
                    //parse byte[] as numeric array
                    byte[] bytes = new byte[array.length()];
                    for (int i = 0; i < array.length(); i++)
                        bytes[i] = (byte) array.getInt(i);
                    value = bytes;
                }
            }

            map.put(key, value);
        }
        return map;
    }
}
