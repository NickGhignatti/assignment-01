package pcd.ass01;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BoidsPlatformThreadSimulator implements BoidsSimulator {
    private static final int FRAMERATE = 25;

    private int framerate;
    private List<Boid> boids;
    private Boolean isRunning;
    private Boolean isFirstTime;
    private List<Thread> threadPool;
    private Optional<BoidsView> view;
    private final BoidsModel model;
    private final CyclicBarrier barrier;
    private final int numberOfProcessors;
    private final CyclicBarrier updateBarrier;

    public BoidsPlatformThreadSimulator(final BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.isFirstTime = true;
        this.view = Optional.empty();
        this.boids = new ArrayList<>();
        this.threadPool = new ArrayList<>();
        this.numberOfProcessors = Runtime.getRuntime().availableProcessors() + 1;
        this.barrier = new CyclicBarrier(this.numberOfProcessors);
        this.updateBarrier = new CyclicBarrier(this.numberOfProcessors + 1);
    }

    @Override
    public void attachView(final BoidsView view) {
        this.view = Optional.of(view);
    }

    @Override
    public void runSimulation() {
        List<Integer> indexes = new ArrayList<>();

        while (true) {
            var t0 = System.currentTimeMillis();
            if (this.isFirstTime && this.isRunning) {
                this.isFirstTime = false;

                int step = this.model.getBoidsNumber() / this.numberOfProcessors;
                for (int i = 0; i < this.numberOfProcessors; i++) {
                    indexes.add(i * step);
                }
                indexes.add(this.model.getBoidsNumber());
                boids = this.model.getBoids();

                for (int i = 0; i < this.numberOfProcessors; i++) {
                    final var finalI = i;
                    threadPool.add(new Thread(() -> {
                        var boidsToCompute = boids.subList(indexes.get(finalI), indexes.get(finalI + 1));
                        while (true) {
                            try {
                                this.updateBarrier.await();
                                for (final Boid b : boidsToCompute)
                                    b.updateVelocity(this.model);
                                this.barrier.await();
                                for (final Boid b : boidsToCompute)
                                    b.updatePos(this.model);
                            } catch (InterruptedException | BrokenBarrierException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }));
                    threadPool.get(i).start();
                }
            }
            this.view.ifPresent(boidsView -> {
                if (this.isRunning) {
                    try {
                        this.updateBarrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }
                }
                boidsView.update(framerate);
            });
            var dtElapsed = System.currentTimeMillis() - t0;
            var frameratePeriod = 1000 / FRAMERATE;

            if (dtElapsed < frameratePeriod) {
                try {
                    Thread.sleep(frameratePeriod - dtElapsed);
                } catch (Exception ignored) {
                }
                framerate = FRAMERATE;
            } else {
                framerate = (int) (1000 / dtElapsed);
            }
        }
    }

    @Override
    public void resumeStop() {
        this.isRunning = !this.isRunning;
    }

    @Override
    public void reset() {
        this.isRunning = false;
    }

    @Override
    public void start() {
        this.isRunning = true;
    }
}
