package com.test.simpletwitter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectionUtils {
    private Context m_context;

    public ConnectionUtils(Context p_context){
        this.m_context = p_context;
    }

    public Boolean IsAnyConnected(){
        ConnectivityManager l_manager = (ConnectivityManager) m_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (l_manager != null) {
            NetworkInfo[] l_info = l_manager.getAllNetworkInfo();
            if (l_info != null) {
                for (int i = 0; i < l_info.length; i++) {
                    if (l_info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}