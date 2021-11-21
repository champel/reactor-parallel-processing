package cat.champel.sandbox;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

public class Benchmark {

    public static final int COUNT = 120;
    public static final int AVERAGE_DELAY = 50;
    public static final int FAIL_EVERY = 25;
    public static final int FAIL_DELAY = 200;

    public static void main(String[] args) throws InterruptedException {
        final var processingTimes = randomArray(COUNT, AVERAGE_DELAY);
        final var schedulers = List.of(
                Pair.of("Single", Schedulers.newSingle("single")),
                Pair.of("10 Parallel", Schedulers.newParallel("10 parallel", 10)),
                Pair.of("100 Parallel", Schedulers.newParallel("100 parallel", 100)),
                Pair.of("Bounded Elastic", Schedulers.boundedElastic()),
                Pair.of("Immediate", Schedulers.immediate()) // Immediate is the last one because blocks the main thread
        );
        final var results = new LinkedHashMap<String, Long>();
        final var threads = new LinkedHashMap<String, Set<String>>();

        final var countDownLatch = new CountDownLatch(schedulers.size());
        for (var schedulerEntry: schedulers) {
            Scheduler scheduler = schedulerEntry.getValue();
            LocalTime start = LocalTime.now();
            Flux.range(0, COUNT)
                    .flatMapSequential(currentInteger -> {
                        final var wait = Math.abs(processingTimes[currentInteger]);
//                        System.out.println(getContext(schedulerEntry, currentInteger) + " -- starting, will wait " + wait);
                        return Mono.just(currentInteger)
                                .map(theInteger -> {
                                    // Here we simulate the function where we can't avoid blocking
                                    registerThread(threads, schedulerEntry.getKey(), Thread.currentThread());
                                    sleep(wait);
                                    if (theInteger % FAIL_EVERY == FAIL_EVERY -1) {
                                        throw new RuntimeException();
                                    }
                                    return getContext(schedulerEntry, theInteger);
                                })
                                .subscribeOn(scheduler)
                                .onErrorContinue((e, i) -> {
                                    System.err.println(getContext(schedulerEntry, (Integer) i) + " failing. Waiting 2000ms...");
                                    sleep(FAIL_DELAY);
                                    System.err.println(getContext(schedulerEntry, (Integer) i) + " continuing");
                                });
                    })
                    .subscribe(
                            x -> {
                                sleep(0);
                                System.out.println(x + " >> delivered by [" + Thread.currentThread().getName() + "] on " + LocalTime.now());
                            },
                            throwable -> System.err.println(throwable.getMessage()),
                            () -> {
                                scheduler.dispose();
                                final var duration = ChronoUnit.MILLIS.between(start, LocalTime.now());
                                System.out.println(schedulerEntry.getKey() + " completed in " + duration + "ms");
                                results.put(schedulerEntry.getKey(), duration);
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
        System.out.println("Used threads");
        System.out.println("==========================================");
        for(var entry: threads.entrySet()) {
            System.out.println(StringUtils.rightPad(entry.getKey(), 30, '.') + StringUtils.leftPad(String.valueOf(entry.getValue().size()), 12, '.'));
        }
        System.out.println("==========================================");
        System.out.println("RANKING");
        System.out.println("==========================================");
        DecimalFormat df = new DecimalFormat("#,###,###");
        for(var entry: results.entrySet()) {
            System.out.println(StringUtils.rightPad(entry.getKey(), 30, '.') + StringUtils.leftPad(df.format(entry.getValue()), 10, '.') + "ms");
        }
        System.out.println();
        System.out.println(StringUtils.rightPad("REFERENCE TOTAL WORK TIME: ", 30, '.') + StringUtils.leftPad(df.format(IntStream.of(processingTimes).sum()),10, '.') + "ms");

    }

    private static String getContext(Pair<String, Scheduler> schedulerEntry, Integer currentInteger) {
        return schedulerEntry.getKey() + " > " + currentInteger + " - [" + Thread.currentThread().getName() + "]";
    }

    private static void sleep(int wait) {
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int[] randomArray(int size, int averageDelay) {
        Random rd = new Random(); // creating Random object
        int[] arr = new int[size];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Math.abs(rd.nextInt(averageDelay * 2)); // storing random integers in an array
        }
        return arr;
    }

    private static void registerThread(Map<String, Set<String>> threads, String schedulerName, Thread thread) {
        if (!threads.containsKey(schedulerName)) {
            threads.put(schedulerName, new HashSet<>());
        }
        threads.get(schedulerName).add(thread.getName());
    }
}