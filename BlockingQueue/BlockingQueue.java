import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class BlockingQueue {

    private final static int TOTAL_MESSAGES = 1000;
    private final static int CAPACITY = 10;
    private final int[] arr;
    private volatile int head;
    private volatile int tail;
    private volatile int size;
    private Condition full;
    private Condition empty;
    private Lock globalLock;

    public BlockingQueue() {
        arr = new int[CAPACITY];
        head = -1;
        tail = -1;
        size = 0;
        globalLock = new ReentrantLock(true);
        full = globalLock.newCondition();
        empty = globalLock.newCondition();
    }

    public boolean add(Integer e) throws InterruptedException {
        try {
            globalLock.lock();
            while (size == CAPACITY) {
                System.out.println("Queue is full. Producers should wait");
                full.await(100, TimeUnit.MILLISECONDS);
            }
            addElement(e);
            empty.signalAll();
            return true;
        } finally {
            globalLock.unlock();
        }
    }

    private void addElement(Integer e) {
        tail = (tail + 1) % CAPACITY;
        arr[tail] = e;
        size++;
    }

    public Integer poll() throws InterruptedException {
        try {
            globalLock.lock();
            while (size == 0) {
                System.out.println("Queue is empty. Consumers should wait");
                empty.await(100, TimeUnit.MILLISECONDS);
            }
            int result = removeElement();
            full.signalAll();
            return result;
        } finally {
            globalLock.unlock();
        }
    }

    private Integer removeElement() {
        head = (head + 1) % CAPACITY;
        int result = arr[head];
        size--;
        return result;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    static class Producer implements Runnable {

        private BlockingQueue q;
        private static AtomicInteger produced = new AtomicInteger(0);

        Producer(BlockingQueue q) {
            this.q = q;
        }

        public void run() {
            while (produced.getAndIncrement() < TOTAL_MESSAGES) {
                try {
                    q.add(produced.get());
                    System.out.println(Instant.now().toEpochMilli() + ",PID:" + Thread.currentThread().getId() + ",+"
                            + produced);
                } catch (InterruptedException e) {
                    System.err.println("PID:" + Thread.currentThread().getId() + "interrupted.");
                    Thread.interrupted();
                }
            }
        }
    }

    static class Consumer implements Runnable {

        private BlockingQueue q;
        private static AtomicInteger consumed = new AtomicInteger(0);

        Consumer(BlockingQueue q) {
            this.q = q;
        }

        public void run() {
            while (consumed.getAndIncrement() < TOTAL_MESSAGES) {
                try {
                    Integer i = q.poll();
                    System.out.println(
                            Instant.now().toEpochMilli() + ",CID:" + Thread.currentThread().getId() + ",(" + i + ")");
                    if (i == 100)
                        break;
                } catch (InterruptedException e) {
                    System.err.println("CID: " + Thread.currentThread().getId() + "interrupted.");
                    Thread.interrupted();
                }
            }
        }
    }

    public static void runDemo() throws InterruptedException {
        BlockingQueue Q = new BlockingQueue();
        int numberOfProducers = 2;
        int numberOfConsumers = 2;
        Thread[] producers = new Thread[numberOfProducers];
        Thread[] consumers = new Thread[numberOfConsumers];

        for (int i = 0; i < numberOfProducers; i++) {
            producers[i] = new Thread(new Producer(Q));
        }
        for (int i = 0; i < numberOfConsumers; i++) {
            consumers[i] = new Thread(new Consumer(Q));
        }
        Instant start = Instant.now();
        Arrays.stream(producers).parallel().forEach(Thread::start);
        Arrays.stream(consumers).parallel().forEach(Thread::start);
        for (int i = 0; i < numberOfProducers; i++) {
            producers[i].join();
        }
        for (int i = 0; i < numberOfConsumers; i++) {
            consumers[i].join();
        }
        Instant end = Instant.now();
        int totalMessages = numberOfProducers * TOTAL_MESSAGES;
        long totalMiliseconds = Duration.between(start, end).toMillis();
        float messagePerSecond = (float) totalMessages / totalMiliseconds;
        System.out.println("Message Per Millisecond = " + messagePerSecond);
    }

    public static void main(String... s) throws InterruptedException {
        BlockingQueue.runDemo();
    }
}