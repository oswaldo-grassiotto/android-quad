package br.com.quadremote;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * @author mrwalbao
 *
 */
public class SettingsActivity extends PreferenceActivity {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        QuadRemote mainActivity = QuadRemote.activity;
        
        if(mainActivity.getSupportedResolutions() == null){
        	String noController = getResources().getString(R.string.no_controller);
        	mainActivity.setSupportedResolutions(new String[]{noController});
        }
        
        addPreferencesFromResource(R.xml.preferences);
        updateResolutions(mainActivity);
    }
	
	private void updateResolutions(QuadRemote mainActivity){
		ListPreference lp = (ListPreference)findPreference("prefSupportedResolutions");
		
    	lp.setEntries(mainActivity.getSupportedResolutions());
    	lp.setEntryValues(mainActivity.getSupportedResolutions());
	}
	
	@Override
	protected void onDestroy() {
		//send a change resolution command to the server when the 
		//settings menu is closed
		
		super.onDestroy();
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String resolutionStr = sharedPrefs.getString("prefSupportedResolutions", "");
		QuadRemote mainActivity = QuadRemote.activity;
		if(!resolutionStr.equals("")){
			mainActivity.setCurrentResolution(resolutionStr);
			mainActivity.sendCommand(5);
		}
	}
}
