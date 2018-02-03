package com.michaelomann.rxjava.akkabridge.scheduler;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import io.reactivex.disposables.Disposable;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class AkkaScheduler extends io.reactivex.Scheduler {
    private final Scheduler scheduler;
    private final ExecutionContext dispatcher;

    public AkkaScheduler(ActorSystem actorSystem) {
        this(actorSystem.scheduler(), actorSystem.dispatcher());
    }

    public AkkaScheduler(Scheduler scheduler, ExecutionContext dispatcher) {
        this.scheduler = scheduler;
        this.dispatcher = dispatcher;
    }

    public Worker createWorker() {
        return new AkkaWorker(scheduler, dispatcher);
    }

    static class AkkaWorker extends Worker {

        private final Scheduler scheduler;
        private final ExecutionContext dispatcher;

        public AkkaWorker(Scheduler scheduler, ExecutionContext dispatcher) {
            this.scheduler = scheduler;
            this.dispatcher = dispatcher;
        }

        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            FiniteDuration duration = FiniteDuration.create(delay, unit);
            Cancellable cancellable = scheduler.scheduleOnce(duration, run, dispatcher);
            return RxDisposables.from(cancellable);
        }

        public void dispose() {

        }

        public boolean isDisposed() {
            return true;
        }
    }
}
