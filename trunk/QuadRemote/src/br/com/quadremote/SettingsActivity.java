package br.com.quadremote;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

/**
 * @author mrwalbao
 *
 */
public class SettingsActivity extends PreferenceActivity {

	public static CharSequence[] resolutions;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(resolutions == null){
        	String noController = getResources().getString(R.string.no_controller);
        	resolutions = new String[]{noController};
        }
        
        addPreferencesFromResource(R.xml.preferences);
        updateResolutions();
    }
	
	private void updateResolutions(){
		ListPreference lp = (ListPreference)findPreference("prefSupportedResolutions");
    	lp.setEntries(resolutions);
    	lp.setEntryValues(resolutions);
	}
}
