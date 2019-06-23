package com.test.simpletwitter;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;
import twitter4j.Status;
import twitter4j.TwitterException;

public class TimelineActivity extends AppCompatActivity {
    private static final int MAX_TWEETS = 50;

    private class AsyncTwitterTimeline extends AsyncTask<Void, Void, List<Status>> {
        @Override
        protected List<twitter4j.Status> doInBackground(Void... params) {
            try {
                return LoginActivity.twitter.getHomeTimeline();
            } catch (TwitterException e) {
                Log.e("Twitter Error", e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<twitter4j.Status> p_timeline) {
            if (p_timeline != null) {
                runOnUiThread(() -> {
                    TestWithTimelineAsObservable (p_timeline);

                    //AddTimelineToView (p_timeline);
                    //ConnectTimelineObservable();
                });
            }
        }
    }

    private void AddStatus(Status p_new) {
        Log.d ("DEBUG", p_new.getUser().getName() + ":" + p_new.getText());
    }

    private void TestWithTimelineAsObservable(List<Status> p_timeline) {
        Observable<Status> l_testObs = Observable.from(p_timeline).map(status -> {
            SystemClock.sleep(500);
            return status;
        });

        l_testObs.subscribeOn(Schedulers.io())
                .subscribe(this::AddStatus);
    }

    private void AddTimelineToView(List<Status> p_timeline) {
        for (Status status : p_timeline) {
            AddStatus(status);
        }
}

    private void ConnectTimelineObservable() {
        ConnectableObservable<Status> l_obs = TimelineObservable.Create().publish();
        l_obs.connect();
        l_obs.subscribeOn(Schedulers.io())
                .sample(200, TimeUnit.MILLISECONDS) // make sure it does not update too fast?
                .subscribe(this::AddStatus);
    }

    private void SetUsername() {
        Intent l_intent = getIntent();
        String l_extra = l_intent.getStringExtra(LoginActivity.EXTRA_USERNAME);

        TextView l_username = findViewById(R.id.username);
        l_username.setText(l_extra);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        SetUsername();
        new AsyncTwitterTimeline().execute();
    }

    // TODO have the "back" button from this activity to LoginActivity also call a logout
    public void OnLogoutAction(View p_view) {
        Intent l_intent = new Intent(this, LoginActivity.class);
        l_intent.putExtra(LoginActivity.EXTRA_LOGOUT, "");
        startActivity(l_intent);
    }
}
