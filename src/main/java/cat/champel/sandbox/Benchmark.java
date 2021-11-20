package cat.champel.sandbox;

import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Benchmark {

    public static final int COUNT = 100;
    public static final int AVERAGE_DELAY = 500;
    public static final int FAIL_EVERY = 25;
    public static final int FAIL_DELAY = 2000;

    public static void main(String[] args) throws InterruptedException {
        final Random random = new Random();
        final Map<String, Scheduler> schedulers = Map.of(
                "Single", Schedulers.newSingle("single"),
                "10 Parallel", Schedulers.newParallel("10 parallel", 10),
                "100 Parallel", Schedulers.newParallel("100 parallel", COUNT)
        );
        final CountDownLatch countDownLatch = new CountDownLatch(schedulers.size());
        final Map<String, Long> results = new LinkedHashMap<>();

        for (var entry: schedulers.entrySet()) {
            Scheduler scheduler = entry.getValue();
            System.out.println("-- " + entry.getKey() + " --");
            LocalTime start = LocalTime.now();
            Flux.range(0, COUNT)
                    .flatMapSequential(aInteger -> {
                        final var wait = Math.abs(random.nextInt(AVERAGE_DELAY * 2));
                        System.out.println("-- starting: " + aInteger + " -- will wait " + wait);
                        return Mono.just(aInteger)
                                .map(theInteger -> {
                                    sleep(wait);
                                    if (theInteger % FAIL_EVERY == FAIL_EVERY -1) {
                                        throw new RuntimeException("! > " + theInteger + " Random fail");
                                    }
                                    return "> " + theInteger;
                                })
                                .subscribeOn(scheduler)
                                .onErrorContinue((e, i) -> {
                                    System.err.println("! " + i + " + failing. Waiting 2000ms...");
                                    sleep(FAIL_DELAY);
                                    System.err.println("> " + i + " + continuing");
                                });
                    })
                    .subscribe(
                            x -> System.out.println(x + " - " + Thread.currentThread().getName() + " - " + LocalTime.now()),
                            throwable -> System.err.println(throwable.getMessage()),
                            () -> {
                                scheduler.dispose();
                                final var duration = ChronoUnit.MILLIS.between(start, LocalTime.now());
                                System.out.println("Completed in " + duration + "ms");
                                results.put(entry.getKey(), duration);
                                countDownLatch.countDown();
                            }
                    );
        }
        countDownLatch.await();
        System.out.println();
        System.out.println("==========================================");
        System.out.println(COUNT + " elements");
        System.out.println(AVERAGE_DELAY + "ms of average delay per element");
        System.out.println("Fail every " + FAIL_EVERY);
        System.out.println(FAIL_DELAY + "ms of delay on fail");
        System.out.println("==========================================");
        System.out.println("RANKING");
        System.out.println("==========================================");
        DecimalFormat df = new DecimalFormat("#,###,###");
        for(var entry: results.entrySet()) {
            System.out.println(StringUtils.rightPad(entry.getKey(), 30, '.') + StringUtils.leftPad(df.format(entry.getValue()), 10, '.') + "ms");
        }
    }

    private static void sleep(int wait) {
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}