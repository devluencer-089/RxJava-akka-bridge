package com.michaelomann.rxjava.akkabridge.scheduler;

import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.dispatch.MessageDispatcher;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
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
        Await.result(terminate, Duration.create(3, SECONDS));
    }

    @Test
    public void itSchedulesWorkOnMainDispatcher() {
        AkkaScheduler scheduler = new AkkaScheduler(actorSystem);

        TestObserver<String> observer = Observable.rangeLong(1, 5)
                .doOnNext(value -> logger.info("received: {}", value))
                .map(value -> Thread.currentThread().getName())
                .subscribeOn(scheduler)
                .test();

        observer.awaitTerminalEvent();
        assertThat(observer.values())
                .hasSize(5)
                .allSatisfy(threadName -> assertThat(threadName).startsWith("default-akka.actor.default-dispatcher"));
    }

    @Test
    public void itSchedulesWorkWithTimedDelayOnMainDispatcher() {
        AkkaScheduler scheduler = new AkkaScheduler(actorSystem);

        AtomicInteger schduledWorkCounter = new AtomicInteger(0);
        RxJavaPlugins.setScheduleHandler(runnable -> {
            schduledWorkCounter.incrementAndGet();
            return runnable;
        });

        TestObserver<String> observer = Observable.interval(10, MILLISECONDS, scheduler)
                .take(3)
                .doOnNext(value -> logger.info("received: {}", value))
                .map(value -> Thread.currentThread().getName())
                .subscribeOn(scheduler)
                .test();

        observer.awaitTerminalEvent();
        assertThat(schduledWorkCounter).hasValue(3);
        assertThat(observer.values())
                .hasSize(3)
                .allSatisfy(threadName -> assertThat(threadName).startsWith("default-akka.actor.default-dispatcher"));
    }

    @Test
    public void itSchedulesWorkOnADedicatedDispatcher() {
        MessageDispatcher customDispatcher = actorSystem.dispatchers().lookup("my-fixed-size-dispatcher");
        AkkaScheduler scheduler = new AkkaScheduler(actorSystem.scheduler(), customDispatcher);

        TestObserver<String> observer = Observable.rangeLong(1, 5)
                .doOnNext(value -> logger.info("received: {}", value))
                .subscribeOn(scheduler)
                .map(value -> Thread.currentThread().getName())
                .subscribeOn(scheduler)
                .test();

        observer.awaitTerminalEvent();
        observer.awaitTerminalEvent();
        assertThat(observer.values())
                .hasSize(5)
                .allSatisfy(threadName -> assertThat(threadName).startsWith("default-my-fixed-size-dispatcher"));
    }

    @Test
    public void itCancelsScheduledWorkWhenDisposed() {
        AkkaScheduler sut = new AkkaScheduler(actorSystem);
        Object spy = spy(new Object());

        Disposable disposable = sut.scheduleDirect(() -> spy.hashCode(), 10, TimeUnit.MILLISECONDS);
        disposable.dispose();

        await().atMost(1, SECONDS).until(() -> {
            verifyZeroInteractions(spy);
        });
    }
}
