package com.michaelomann.rxjava.akkabridge.scheduler;

import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.dispatch.MessageDispatcher;
import io.reactivex.Observable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static org.mockito.MockitoAnnotations.initMocks;

public class AkkaSchedulerTest {

    private static final Logger logger = LoggerFactory.getLogger(AkkaSchedulerTest.class);

    private ActorSystem actorSystem;

    @Before
    public void init() {
        initMocks(this);
        actorSystem = ActorSystem.create();
    }

    @After
    public void shutdown() throws Exception {
        initMocks(this);
        Future<Terminated> terminate = actorSystem.terminate();
        Await.result(terminate, Duration.create(3, TimeUnit.SECONDS));
    }

    @Test
    public void itSchedulesWork() {
        AkkaScheduler scheduler = new AkkaScheduler(actorSystem);

        Observable.rangeLong(1, 5)
                .doOnNext(value -> logger.info("received: {}", value))
                .subscribeOn(scheduler)
                .blockingSubscribe();
    }

    @Test
    public void itSchedulesWorkWithTimedDelay() {
        AkkaScheduler scheduler = new AkkaScheduler(actorSystem);

        Observable.interval(1, TimeUnit.SECONDS, scheduler)
                .take(3)
                .doOnNext(value -> logger.info("received: {}", value))
                .subscribeOn(scheduler)
                .blockingSubscribe();
    }
    @Test
    public void itSchedulesWorkOnADedicatedDispatcher() {
        MessageDispatcher customDispatcher = actorSystem.dispatchers().lookup("my-fixed-size-dispatcher");
        AkkaScheduler scheduler = new AkkaScheduler(actorSystem.scheduler(), customDispatcher);

        Observable.rangeLong(1, 5)
                .doOnNext(value -> logger.info("received: {}", value))
                .subscribeOn(scheduler)
                .blockingSubscribe();
    }
}
