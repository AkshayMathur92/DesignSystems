import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class BlockingQueue {

    private final static int MAX_WAIT_MILIS = 1000;
    private final static int CAPACITY = 5;
    private final transient int[] arr;
    private transient int head;
    private transient int tail;
    private transient int size;
    private Lock producerLock;
    private Condition full;
    private Lock consumerLock;
    private Condition empty;
    private Lock globalLock;

    public BlockingQueue() {
        arr = new int[CAPACITY];
        producerLock = new ReentrantLock();
        consumerLock = new ReentrantLock();
        head = -1;
        tail = -1;
        size = 0;
        globalLock = new ReentrantLock();
        full = globalLock.newCondition();
        empty = globalLock.newCondition();
    }

    public boolean add(Integer e) throws InterruptedException {
        globalLock.lock();
        try {
            while (size == CAPACITY) {
                System.out.println("Queue is full. Producers should wait");
                full.await();
            }
            addElement(e);
            empty.signalAll();
            return false;
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
        globalLock.lock();
        try {
            while (size == 0) {
                System.out.println("Queue is empty. Consumers should wait");
                empty.await();
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
        // TODO Auto-generated method stub
        return false;
    }

    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    static class Producer implements Runnable {

        private BlockingQueue q;
        private long id;
        private Random rng;

        Producer(BlockingQueue q) {
            this.q = q;
            this.id = Thread.currentThread().getId();
            rng = new Random();
        }

        public void run() {
            while (true) {
                int random = rng.nextInt(MAX_WAIT_MILIS);
                try {
                    q.add(random);
                    System.out.println("Producer id:" + id + " produced " + random);
                } catch (InterruptedException e) {
                    System.err.println("Producer id:" + id + "interrupted.");
                    Thread.interrupted();
                }
            }
        }
    }

    static class Consumer implements Runnable {

        private BlockingQueue q;
        private long id;

        Consumer(BlockingQueue q) {
            this.q = q;
            this.id = Thread.currentThread().getId();
        }

        public void run() {
            while (true) {
                try {
                    Integer i = q.poll();
                    System.out.println("Consumer id:" + id + " consumed " + i);
                } catch (InterruptedException e) {
                    System.err.println("Consuner id: " + id + "interrupted.");
                    Thread.interrupted();
                }
            }
        }
    }

    public static void runDemo() throws InterruptedException {
        BlockingQueue Q = new BlockingQueue();

        int numberOfProducers = 2;
        int numberOfConsumers = 1;
        Thread[] producers = new Thread[numberOfProducers];
        Thread[] consumers = new Thread[numberOfConsumers];

        for (int i = 0; i < numberOfProducers; i++) {
            producers[i] = new Thread(new Producer(Q));
            producers[i].join();

        }
        for (int i = 0; i < numberOfConsumers; i++) {
            consumers[i] = new Thread(new Consumer(Q));
            consumers[i].join();
        }
        Arrays.stream(producers).forEach(Thread::start);
        Arrays.stream(consumers).forEach(Thread::start);
    }

    public static void main(String... s) throws InterruptedException {
        BlockingQueue.runDemo();
    }
}