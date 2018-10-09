package com.vecolsoft.deligo.Utils;

import android.content.SharedPreferences;

public class Utils {

    public static String getUserEmailPrefes(SharedPreferences prefs){
        return prefs.getString("email","");
    }

    public static String getUserPassPrefes(SharedPreferences prefs){
        return prefs.getString("pass","");
    }

    public static boolean getValuePreference(SharedPreferences prefs) {
        return  prefs.getBoolean("estado_switch", false);
    }

    public static void removesharedpreferencies(SharedPreferences prefs){
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("email");
        editor.remove("pass");
        editor.remove("estado_switch");
        editor.apply();
    }
}
