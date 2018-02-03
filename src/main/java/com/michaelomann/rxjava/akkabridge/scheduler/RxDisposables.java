package com.michaelomann.rxjava.akkabridge.scheduler;

import akka.actor.Cancellable;
import io.reactivex.disposables.Disposable;

public class RxDisposables {

    public static Disposable from(final Cancellable cancellable) {
        return new Disposable() {
            public void dispose() {
                if (!cancellable.isCancelled())
                    cancellable.cancel();
            }

            public boolean isDisposed() {
                return cancellable.isCancelled();
            }
        };
    }
}
