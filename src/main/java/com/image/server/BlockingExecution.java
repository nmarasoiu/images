package com.image.server;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class BlockingExecution {
    private final Scheduler scheduler;

    public BlockingExecution(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public <T> Mono<T> scheduleBlockingMono(Callable<Mono<T>> callable) {
        return Mono.fromCallable(callable)
                .subscribeOn(scheduler)
                .flatMap(Function.identity());
    }

    public <T> Mono<T> scheduleBlockingCode(Callable<T> callable) {
        return scheduleBlockingMono(() -> Mono.just(callable.call()));
    }

    public <T> Flux<T> scheduleBlockingFlux(Callable<Flux<T>> callable) {
        return scheduleBlockingCode(callable)
                .flux()
                .flatMap(Function.identity());
    }

}