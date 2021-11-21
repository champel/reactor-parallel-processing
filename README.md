## About This project

This project test the behaviour of `flatMapSequential` processing elements in parallel for cases when you
can't avoid blocking in the processing, and you can have delay.

It also includes examples of managing failures that produce delays. 

### Execute the benchmark

  ```sh
  mvn exec:java
  ```
Result example

```
-- Single --
-- starting: 0 -- will wait 55
-- starting: 1 -- will wait 62
...
Immediate -- starting: 118 -- will wait 70
Single > 118 - single-1 -  >> single-1 on 12:53:19.468815
Immediate > 118 - main -  >> main on 12:53:19.528341
Immediate -- starting: 119 -- will wait 81
Single > 119 - single-1 -  >> single-1 on 12:53:19.552907
Single completed in 7083ms
Immediate > 119 - main -  >> main on 12:53:19.613180
Immediate completed in 6939ms

==========================================
120 elements
50ms of average delay per element
Fail every 25
200ms of delay on fail
==========================================
Used threads
==========================================
Single...................................1
10 Parallel.............................10
100 Parallel...........................100
Bounded Elastic.........................80
Immediate................................1
==========================================
RANKING
==========================================
100 Parallel.........................330ms
Bounded Elastic......................347ms
10 Parallel........................1,144ms
Single.............................7,083ms
Immediate..........................6,939ms

REFERENCE TOTAL WORK TIME: ........5,828ms

Process finished with exit code 0

```