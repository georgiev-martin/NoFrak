package org.apache.cordova;

import java.util.ArrayList;

import android.util.Log;

/**
 * This class represents the NoFrakStore.
 *
 * @author
 * @since 7/23/2013
 */
public class NoFrakStore {
    
    private static String TAG = "NoFrakStore";
    
    private static NoFrakStore store;
    private static ArrayList<NoFrakEntry> entries;
    
    /**
     * CONSTRUCTOR
     */
    private NoFrakStore() {
        entries = new ArrayList<NoFrakEntry>();
    }
    
    /**
     * Get the NoFrakStore.
     */
    public static NoFrakStore getInstance() {
        if (store == null) {
            store = new NoFrakStore();
        }
        
        return store;
    }
    
    /**
     * Add a new NoFrakEntry to the NoFrakStore.
     * @param secureToken : the SecureToken.
     * @param callbackId : the callbackId.
     * @param result : the return result.
     */
    protected static void add(String secureToken, String callbackId, String result) {
        if (store == null) {
            store = getInstance();
        }
        
        // Get the NoFrakEntry.
        for (NoFrakEntry entry : entries) {
            if (entry.getCapability().equals(secureToken)) {
                // This is the right entry.
                
                // Get the ReturnResult list.
                ArrayList<NoFrakEntryReturnResult> list = entry.getResultList();
                
                // Add the new return result.
                NoFrakEntryReturnResult nferr = new NoFrakEntryReturnResult(callbackId, result);
                list.add(nferr);
                
                // Update the list.
                entry.setResultList(list);
                
                break;
            }
        }
    }
    
    /**
     * Add a new NoFrakEntry to the NoFrakStore.
     * @param origin : the origin.
     * @param secureToken : the SecureToken.
     */
    protected static void add(String origin, String secureToken) {
        Log.d(TAG, "Origin=" + origin + " SecureToken=" + secureToken);
        
        if (store == null) {
            store = getInstance();
        }
        
        boolean isIn = false;
        for (NoFrakEntry entry : entries) {
            if (entry.getOrigin().equals(origin)) {
                isIn = true;
                break;
            }
        }
        
        if (!isIn) {
            NoFrakEntry nfe = new NoFrakEntry(origin, secureToken);
            store.add(nfe);
        }
    }
    
    /**
     * Add a new NoFrakEntry to the NoFrakStore.
     * @param nfe : a new NoFrakEntry.
     */
    private static void add(NoFrakEntry nfe) {
        if (store == null) {
            store = getInstance();
        }
        
        boolean isIn = false;
        for (NoFrakEntry entry : entries) {
            if (entry.getOrigin().equals(nfe.getOrigin())) {
                isIn = true;
                break;
            }
        }
        
        if (!isIn) {
            entries.add(nfe);
        }
    }
    
    /**
     * Add a return result to the NoFrakStore.
     * @param msg : the return result.
     */
    protected static void putMsg(String msg) {
        if (store == null) {
            Log.d(TAG, "The NoFrakStore is not initialized!");
            return;
        }
        
        for (NoFrakEntry entry : entries) {
            // Each domain has a list of return results associated with it.
            ArrayList<NoFrakEntryReturnResult> results = entry.getResultList();
            Log.d(TAG, "# of Results=" + results.size() + " msg=" + msg);
            
            /* 
             * Each return result has its own callbackId.
             * While we CANNOT trust the callbackId value on the uplink, we CAN trust it on the downlink.
             * Hence, we can bind the downlink call to the return result unambiguously.
             */
            for (NoFrakEntryReturnResult result : results) {
                String callbackId = result.getReference();
                Log.d(TAG, "NoFrakEntryReturnResult-callbackId=" + callbackId);
                if (msg.indexOf(callbackId) != -1) {
                    Log.d(TAG, "Bind the two messages together!");
                    // bind the two together.
                    result.setResult(msg);
                    return;
                }
            }
        }
    }
    
    /**
     * Pop a message for this domain.
     * @param secureToken : the SecureToken.
     */
    protected static String getMsg(String secureToken) {
        if (store == null) {
            return null;
        }
        
        for (NoFrakEntry entry : entries) {
            if (entry.getCapability().equals(secureToken)) {
                // Each domain has a list of return results associated with it.
                ArrayList<NoFrakEntryReturnResult> results = entry.getResultList();

                if ((results == null) || (results.size() == 0)) {
                    return null;
                }
                
                for(int index = 0; index < results.size(); index++) {
                    // Get the next result in the list
                    NoFrakEntryReturnResult result = results.get(index);
                    
                    // Get the response.
                    String response = result.getResult();
                    
                    if (response == null) {
                        continue;
                    } else {
                        // Reset the response.
                        result.setResult(null);
                        return response;
                    }
                }
                
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Make sure that we know this SecureToken.
     * @param secureToken : the SecureToken we are verifying.
     */
    protected static boolean contains(String secureToken) {
        for(NoFrakEntry entry : entries) {
            if(entry.getCapability().equals(secureToken)) {
                return true;
            }
        }
        return false;
    }
}