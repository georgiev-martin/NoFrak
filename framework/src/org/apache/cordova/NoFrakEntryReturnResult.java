package org.apache.cordova;

import android.util.Log;

/**
 * This class represents a single NoFrakEntryReturnResult.
 *
 * @author
 * @since 7/27/2013
 */
public class NoFrakEntryReturnResult {
    
    private String TAG = "NoFrakEntryReturnResult";
    
    private String reference;
    private String result;
    
    /**
     * CONSTRUCTOR
     *
     * This constructor represents a NoFrakEntryReturnResult.
     * @param reference : the reference id associated with the API call.
     * @param result : the result of the API call.
     */
    protected NoFrakEntryReturnResult(String reference, String result) {
        this.reference = reference;
        this.result = result;
        
        Log.d(TAG, "Reference=" + reference + " Result=" + result);
    }
    
    /**
     * Get the reference id associated with the API call.
     */
    protected String getReference() {
        return this.reference;
    }
    
    /**
     * Set the reference id associated with the API call.
     * @param reference : the reference id associated with the API call.
     */
    protected void setReference(String reference) {
        this.reference = reference;
    }
    
    /**
     * Get the result of the API call.
     */
    protected String getResult() {
        return this.result;
    }
    
    /**
     * Set the result of the API call.
     * @param result : the result of the API call.
     */
    protected void setResult(String result) {
        this.result = result;
    }
}