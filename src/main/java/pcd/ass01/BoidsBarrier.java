package pcd.ass01;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoidsBarrier {
    private final int numberOfSync;
    private final ReentrantLock lock;
    private final Condition condition;
    private int generation = 0;
    private int numberOfWait;

    public BoidsBarrier(final int numberOfSync) {
        this.numberOfSync = numberOfSync;
        this.numberOfWait = 0;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    public void await() throws InterruptedException {
        lock.lock();
        try {
            int currentGeneration = generation;
            numberOfWait++;

            if (numberOfWait == numberOfSync) {

                numberOfWait = 0;
                generation++;
                condition.signalAll();
            } else {
                while (currentGeneration == generation) {
                    condition.await();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void breakBarrier() {
        lock.lock();
        try {
            numberOfWait = 0;
            generation++;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
