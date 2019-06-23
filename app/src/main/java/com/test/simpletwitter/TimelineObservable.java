package com.test.simpletwitter;

import rx.Observable;
import twitter4j.*;

public class TimelineObservable {
    public static Observable<Status> Create() {
        return Observable.create(subscriber -> {
            final TwitterStream l_stream = new TwitterStreamFactory().getInstance();

            l_stream.addListener(new StatusAdapter() {
                public void onStatus(Status status) {
                    subscriber.onNext(status);
                }

                public void onException(Exception ex) {
                    subscriber.onError(ex);
                }
            });

            l_stream.sample();
        });
    }
}
