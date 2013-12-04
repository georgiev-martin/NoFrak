/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova;

import android.os.Build;
import android.webkit.JavascriptInterface;
import android.util.Log;

import org.apache.cordova.api.PluginManager;
import org.apache.cordova.api.PluginResult;

import org.json.JSONException;


/**
 * Contains APIs that the JS can call. All functions in here should also have
 * an equivalent entry in CordovaChromeClient.java, and be added to
 * cordova-js/lib/android/plugin/android/promptbasednativeapi.js
 */
/* package */ class ExposedJsApi {
    
    private PluginManager pluginManager;
    private NativeToJsMessageQueue jsMessageQueue;
    private static String TAG = "ExposedJsApi";
    
    public ExposedJsApi(PluginManager pluginManager, NativeToJsMessageQueue jsMessageQueue) {
        this.pluginManager = pluginManager;
        this.jsMessageQueue = jsMessageQueue;
    }

    @JavascriptInterface
    public String exec(String service, String action, String callbackId, String arguments, String secureToken) throws JSONException {
       
        Log.d(TAG, "service=" + service);
        Log.d(TAG, "action=" + action);
        Log.d(TAG, "callbackId=" + callbackId);
        Log.d(TAG, "secureToken=" + secureToken);
        
        // If the arguments weren't received, send a message back to JS.  It will switch bridge modes and try again.  See CB-2666.
        // We send a message meant specifically for this case.  It starts with "@" so no other message can be encoded into the same string.
        if (arguments == null) {
            return "@Null arguments.";
        }

        jsMessageQueue.setPaused(true);
        
        // Get the CapabilityManagerImpl and make sure that the SecureToken is correct.
        CapabilityManagerImpl capabilityManager = CapabilityManagerImpl.getInstance(null);
        if(!capabilityManager.isACorrectSecureToken(secureToken)) {
            Log.e(TAG, secureToken + " is NOT the correct secure token!");
            jsMessageQueue.setPaused(false);
            return null;
        }
         
        boolean wasSync = pluginManager.exec(service, action, callbackId, arguments);
        String ret = "";
        // If this call is synchronous, then popAndEncode the message.
        if (!NativeToJsMessageQueue.DISABLE_EXEC_CHAINING || wasSync) {
            ret = jsMessageQueue.popAndEncode();
        }
          
        Log.d(TAG, "wasSync=" + wasSync);
        Log.d(TAG, "isSync=" + ((!NativeToJsMessageQueue.DISABLE_EXEC_CHAINING || wasSync)));
        Log.d(TAG, "ret=" + ret);
        
        /*
         * Some API calls are syncronous, yet marked as asyncronous. e.g. NetworkStatus.
         */
        if ((ret == null) || ret.equals("") || (wasSync == false)) {
            // Async calls cannot reuse the same channel. Hence, we need to push them through the NoFrakStore.
            Log.d(TAG, "Log callbackId=" + callbackId);
            NoFrakStore.add(secureToken, callbackId, ret);
        }
        jsMessageQueue.setPaused(false);
        return ret;
    }
    
    @JavascriptInterface
    public void setNativeToJsBridgeMode(String secureToken, int value) {
        // Get the CapabilityManagerImpl and make sure that the SecureToken is correct.
        CapabilityManagerImpl capabilityManager = CapabilityManagerImpl.getInstance(null);
        if(!capabilityManager.isACorrectSecureToken(secureToken)) {
            Log.e(TAG, secureToken + " is NOT the correct secure token!");
            return;
        }
        
        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Do not let API < 17 to switch to addJavascriptInterace.
            // In this case the bridge is not activated. Hence, even if we switch, it won't work.
            return;
        }
        
        jsMessageQueue.setBridgeMode(value);
    }
    
    @JavascriptInterface
    public String retrieveJsMessages(String secureToken) {
        // Get the CapabilityManagerImpl and make sure that the SecureToken is correct.
        CapabilityManagerImpl capabilityManager = CapabilityManagerImpl.getInstance(null);
        if(!capabilityManager.isACorrectSecureToken(secureToken)) {
            Log.e(TAG, secureToken + " is NOT the correct secure token!");
            return null;
        }
        
        // We recognize the secure token. This domain is allows to pop messages off the stack. Get the next JavaScript message.
        String msg = jsMessageQueue.popAndEncode();

        if ((msg == null) || (msg.equals(""))){
            return null;
        }
        
        /* 
         * e.g.
         * 662 S01 Contacts1122844108 [
         * {"displayName":"User1","id":"1","rawId":"1","phoneNumbers":[{"type":"mobile","value":"111-111-1111","id":"1","pref":false}]},
         * {"displayName":"User2","id":"2","rawId":"2","phoneNumbers":[{"type":"mobile","value":"222-222-2222","id":"2","pref":false}]},
         * {"displayName":"User3","id":"3","rawId":"3","phoneNumbers":[{"type":"mobile","value":"333-333-3333","id":"3","pref":false}]}]
         */
        
        /* 
         * PhoneGap doesn't follow it's own standards =(
         * Few API calls (e.g. playback a sound) result in messages unbound to a callbackId.
         */
        
        Log.d(TAG, "Message=" + msg);
        
        if ((msg.indexOf(" Jcordova.require('") != -1) || (msg.indexOf(" Jcordova.fireDocumentEvent('") != -1)){
            // This event is not sensitive. For the most part we can do even without it. 
            Log.d(TAG, "Skipping along.");
            return msg;
        }

        // Most of API calls ARE bound to a callbackId, though.
        // Add the message to the NoFrakStore.
        NoFrakStore.putMsg(msg);
        
        // Pop a message from the NoFrakStore for this domain.
        String result = NoFrakStore.getMsg(secureToken);
        
        Log.d(TAG, "Result=" + result);
        
        return result;
    }
}
