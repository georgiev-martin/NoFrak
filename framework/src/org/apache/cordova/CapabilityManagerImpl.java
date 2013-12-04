package org.apache.cordova;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Set;
import java.util.TreeSet;
import java.util.Formatter;

import java.security.SecureRandom;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;

import android.util.Log;

/**
 * The purpose of this class is to filter calls from JavaScript to davice APIs and authorize only those calls which are whitelisted.
 *
 * NOTE: We employ localStorage in order to leverage SOP.
 * NOTE: SOP restricts access to the SecureToken we assign to each domain to the domian itself!
 * NOTE: SOP is crucial for the security of the inbound call!
 * 
 * @author 
 * @since 7/20/2013
 */
public class CapabilityManagerImpl {

	private static String TAG = "CapabilityManagerImpl";

    private static WebStorageHelperImpl webStorage;
	private static CapabilityManagerImpl capabilityManager;
    
	private static boolean isStrict;
    
    private final static int LOWER_BOUND = 100000000;
    private final static int UPPER_BOUND = 1000000000;
	
	/**
	 * No public invokations of the constructor.
	 */
	private CapabilityManagerImpl() {
		isStrict = false;
		Log.i(TAG, "You are exercising an INsecure CapabilityManager! To change it to a secure CapabilityManager call the init() method!");
	}

	/**
	 * Get the CapabilityManagerImpl.
     * @param context : the application context.
	 */
	public static CapabilityManagerImpl getInstance(Context context) {
		if(capabilityManager == null) {
			Log.i(TAG, "Creating a new CapabilityManagerImpl!");
			capabilityManager = new CapabilityManagerImpl();
		}
        
        if(webStorage == null) {
            Log.i(TAG, "Creating a new WebStorageHelperImpl!");
            webStorage = new WebStorageHelperImpl(context);
        }
        
		return capabilityManager;
	}

	/**
	 * Add unforgeable tokens for all domains which should be granted access to Cordova's JavaScript functionality. 
	 */
	public void init(Activity action) {
		isStrict = true;
		Log.i(TAG, "You are exercising a secure CapabilityManager!");
		setupSecureTokens(action);
	}

	/**
	 * Check if the SecureToken handed in by the JavaScript side matches the SecureToken assigned to that domain.
     *
	 * NOTE: JavaScript document.location won't give you the right domain due to frame confusion (when invoked from within an iframe).
	 * 	     Moreover, JavaScript can pass in any domain it wants --- the argument is under the attacker's control. 
     *
     * NOTE: By inserting the SecureToken in localStorage, we leverage SOP protection.
     *
	 * @param secureToken : The SecureToken JavaScript got by calling localStorage.getItem("SecureToken").
	 */
	public boolean isACorrectSecureToken(String secureToken) {
		// If the CapabilityManager is not strict, then allow all calls from JavaScript.
		if(!isStrict) {
			return true;
		}
        
		// However, if the CapabilityManager is strict, make sure that we assigned this SecureToken.
        if(NoFrakStore.contains(secureToken)) {
            return true;
        }
        
		// If we do not know the SecureToken, but the CapabilityManager is strict, discard this call from JavaScript.
		return false;
	}

	/**
	 * Add a SecureToken to the NoFrakStore and sync with localStorage 
     * for all domains which should be able to access Cordova's JavaScript functionality.
	 */
	private void setupSecureTokens(Activity action) {
		
		if (action == null) {
			Log.i(TAG, "There is no activity. Is this on the lock screen?");
			return;
		}

		int id = action.getResources().getIdentifier("config", "xml", action.getPackageName());
		if (id == 0) {
			id = action.getResources().getIdentifier("cordova", "xml", action.getPackageName());
			Log.i(TAG, "config.xml missing, reverting to cordova.xml");
		}
		if (id == 0) {
			Log.i(TAG, "cordova.xml missing. Ignoring...");
			return;
		}

		XmlResourceParser xml = action.getResources().getXml(id);
		int eventType = -1;
		while (eventType != XmlResourceParser.END_DOCUMENT) {
			if (eventType == XmlResourceParser.START_TAG) {
				String strNode = xml.getName();

				if (strNode.equals("access")) {
					String origin = xml.getAttributeValue(null, "origin");
					Log.i(TAG, "Origin=" + origin);

					// TODO: Should we consider subdomains?
					// String subdomains = xml.getAttributeValue(null, "subdomains");

					if(origin.equals(".*")) {
                        // Don't allow wild-card whitelisting.
						continue;
					}
                    
                    // Generate a fresh SecureToken
					String secureToken = getFreshTokenValue();

                    // Sync up with database.
                    String dbName = setUpDatabase(origin);
                    webStorage.setItem(dbName, "SecureToken", secureToken);

                    // Add a new NoFrakEntry to the NoFrakStore.
                    NoFrakStore.add(origin, secureToken);
				}
			}

			try {
				eventType = xml.next();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Mark the CapabilityManager as NOT strict.
	 */
	private void reset() {
		isStrict = false;
	}

	/**
	 * Get the domain from the origin specified in the cordova.xml/config.xml file
	 * @param origin : the origin specified in the cordova.xml/config.xml file
	 * @return : the domain corresponding to the specified origin. e.g. "google.com"
	 */
	private static String getDomain(String origin) {
		String domain = null;

		try {
			URL url = new URL(origin);
			domain = url.getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		if(domain.endsWith("*")) {
			domain = domain.substring(0, domain.length() - 1);
		} 
		
		return domain;
	}
  
    /**
      * Given an origin, set up the localStorage database.
      * @param origin : the origin whose db we should set up. e.g. http://www.example.com
      */
    private String setUpDatabase(String origin) {
        
        // Put the origin into this format: "https_www.example.com_0.localstorage"; file__0.localstorage
        boolean shouldCreateDB = false;
        String dbName = null;
        if(origin.toLowerCase().startsWith("https://")) {
            shouldCreateDB = true;
            dbName = "https_" + origin.substring(("https://").length());
        } else if(origin.toLowerCase().startsWith("http://")) {
            shouldCreateDB = true;
            dbName = "http_" + origin.substring(("http://").length());
        } else if(origin.equals("127.0.0.1") || origin.equals("127.0.0.1*")) {
            shouldCreateDB = true;
            dbName = "file_";
        }
        
        if(shouldCreateDB) {
            // Set suffix.
            dbName = dbName + "_0.localstorage";
            
            // DEBUG:
            Log.i(TAG, "Database=" + dbName);
            
            // Create the localStorage for this origin.
            webStorage.createDatabase(dbName);
        }
        
        return dbName;
    }

	/**
	 * Get a fresh token value.
	 * @return : a fresh token value.
	 */
	private String getFreshTokenValue() {
        SecureRandom ranGen = new SecureRandom();
        int rand = ranGen.nextInt(UPPER_BOUND);
        do {
            rand = ranGen.nextInt(UPPER_BOUND);
        } while(rand < LOWER_BOUND);
        
		return "" + rand;
	}
}
