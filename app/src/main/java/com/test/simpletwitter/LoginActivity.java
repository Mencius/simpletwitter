package com.test.simpletwitter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.os.StrictMode;

import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class LoginActivity extends AppCompatActivity {
    private static final String TWITTER_CONSUMER_KEY = "IVWDHvO6fhghGfgSurBpyxSSU";
    private static final String TWITTER_CONSUMER_SECRET = "4AD61SRE36t6icrreO6AQHO0bFVE57g1WFjN3tNUX1cXkeDQYt";
    private static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
    private static final String PREF_KEY_TWITTER_LOGIN = "twitter_login";
    private static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    private static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    private static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";

    public static final String EXTRA_USERNAME = "com.test.simpletwitter.USERNAME";
    public static final String EXTRA_LOGOUT = "com.test.simpletwitter.LOGOUT";


    private static RequestToken m_twitterToken;

    public static Twitter twitter;

    private SharedPreferences m_sharedPrefs;

    // Necessary since Android does not allow requests to be done on main thread
    // Is there a less verbose way to do this?
    private class AsyncTwitterLogin extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                m_twitterToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
            } catch (TwitterException e) {
                Log.e("Twitter Error (1)", e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            runOnUiThread(() -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m_twitterToken.getAuthenticationURL()))));
        }
    }

    private class AsyncTwitterToken extends AsyncTask<String, Void, String> {
        private ProgressDialog m_progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            m_progress = new ProgressDialog(LoginActivity.this);
            m_progress.setMessage(getResources().getString(R.string.progress_loggingin));
            m_progress.setIndeterminate(false);
            m_progress.setCancelable(false);
            m_progress.show();
        }

        @Override
        protected String doInBackground(String... p_verifier) {
            try {
                AccessToken l_token = twitter.getOAuthAccessToken(m_twitterToken, p_verifier[0]);

                SaveTokens (l_token);

                long l_userID = l_token.getUserId();
                User l_user = twitter.showUser(l_userID);

                return l_user.getName();
            } catch (TwitterException e) {
                Log.e("Twitter Error (4)", e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String p_username) {
            m_progress.dismiss();
            if (p_username != null) {
                runOnUiThread(() -> StartTimelineActivity(p_username));
            }
        }
    }

    /*private void TwitterLoginMainThread() {
        try {
            m_twitterToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m_twitterToken.getAuthenticationURL())));
        } catch (TwitterException e) {
            Log.e("Twitter Error", "> " + e.getMessage());
        }
    }

    private void TwitterAccessMainThread(String p_verifier) {
        try {
            AccessToken l_accessToken = twitter.getOAuthAccessToken(m_twitterToken, p_verifier);

            SaveTokens (l_accessToken);

            long l_userID = l_accessToken.getUserId();
            User l_user = twitter.showUser(l_userID);

            StartTimelineActivity(l_user.getName());
        } catch (Exception e) {
            Log.e("Twitter Error (3)", "> " + e.toString());
        }
    }*/

    private boolean IsTwitterLoggedIn() {
        return m_sharedPrefs.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    private AccessToken GetAccessTokenFromPrefs() {
        String l_oauthToken = m_sharedPrefs.getString(PREF_KEY_OAUTH_TOKEN, "");
        String l_oauthSecret = m_sharedPrefs.getString(PREF_KEY_OAUTH_SECRET, "");

        return new AccessToken(l_oauthToken, l_oauthSecret);
    }

    private void SaveTokens(AccessToken p_accessToken) {
        SharedPreferences.Editor l_editor = m_sharedPrefs.edit();
        l_editor.putString(PREF_KEY_OAUTH_TOKEN, p_accessToken.getToken());
        l_editor.putString(PREF_KEY_OAUTH_SECRET, p_accessToken.getTokenSecret());
        l_editor.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
        l_editor.commit();
    }

    private void ClearTokens() {
        Log.d ("DEBUG", "ClearTokens");

        SharedPreferences.Editor l_editor = m_sharedPrefs.edit();
        l_editor.remove(PREF_KEY_OAUTH_TOKEN);
        l_editor.remove(PREF_KEY_OAUTH_SECRET);
        l_editor.remove(PREF_KEY_TWITTER_LOGIN);
        l_editor.commit();
    }

    private void StartTimelineActivity(String p_username) {
        Intent l_intent = new Intent(this, TimelineActivity.class);
        l_intent.putExtra(EXTRA_USERNAME, p_username);
        startActivity(l_intent);
    }

    private void ReAuthenticate() {
        if (twitter == null) {
            twitter = TwitterFactory.getSingleton();
            twitter.setOAuthConsumer(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET);
        }

        AccessToken l_accessToken = GetAccessTokenFromPrefs();
        twitter.setOAuthAccessToken(l_accessToken);

        try {
            long l_userID = l_accessToken.getUserId();
            User l_user = twitter.showUser(l_userID);

            StartTimelineActivity(l_user.getName());
        } catch (Exception e) {
            Log.e("Twitter Error (2)", e.toString());
            ClearTokens();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // This allows network requests on main thread but seems dirty
        /*if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }*/

        m_sharedPrefs = getApplicationContext().getSharedPreferences("SimpleTwitterPref", 0);

        Intent l_intent = getIntent();
        if (l_intent.hasExtra(EXTRA_LOGOUT)) {
            Log.d ("DEBUG", "OnCreate - logging out, back from TimelineActivity");
            ClearTokens();
        }

        if (!IsTwitterLoggedIn()) {
            Uri l_uri = getIntent().getData();
            if (l_uri != null && l_uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                Log.d ("DEBUG", "OnCreate - back from Twitter authenticate");

                String l_verifier = l_uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
                new AsyncTwitterToken ().execute(l_verifier);
                //TwitterAccessMainThread (l_verifier);
            }
        } else {
            Log.d ("DEBUG", "OnCreate - already logged in, get tokens in shared prefs");

            ReAuthenticate();
        }
    }

    public void OnLoginAction(View p_view) {
        if (!IsTwitterLoggedIn()) {
            Log.d ("DEBUG", "OnLoginAction - start Twitter authentication");

            ConfigurationBuilder l_builder = new ConfigurationBuilder();
            l_builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            l_builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
            Configuration l_twitterConf = l_builder.build();

            TwitterFactory factory = new TwitterFactory(l_twitterConf);
            twitter = factory.getInstance();

            new AsyncTwitterLogin().execute();
            //TwitterLoginMainThread();
        } else {
            Log.d ("DEBUG", "OnLoginAction - already logged in, get tokens in shared prefs");

            ReAuthenticate();
        }
    }
}
