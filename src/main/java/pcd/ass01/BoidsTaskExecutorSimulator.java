package pcd.ass01;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

public class BoidsTaskExecutorSimulator implements BoidsSimulator {
    private static final int FRAMERATE = 25;

    private int framerate;
    private Boolean isRunning;
    private Optional<BoidsView> view;
    private final BoidsModel model;
    private final CyclicBarrier barrier;
    private final int numberOfProcessors;
    private List<Runnable> workers;
    private final CyclicBarrier updateBarrier;

    public BoidsTaskExecutorSimulator(final BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.view = Optional.empty();
        this.workers = new ArrayList<>();
        this.numberOfProcessors = Runtime.getRuntime().availableProcessors() + 1;
        this.barrier = new CyclicBarrier(this.numberOfProcessors);
        this.updateBarrier = new CyclicBarrier(this.numberOfProcessors + 1);
    }

    @Override
    public void attachView(final BoidsView view) {
        this.view = Optional.of(view);
    }

    private void initThread() {
        List<Integer> indexes = new ArrayList<>();
        int step = this.model.getBoidsNumber() / this.numberOfProcessors;
        for (int i = 0; i < this.numberOfProcessors; i++) {
            indexes.add(i * step);
        }
        indexes.add(this.model.getBoidsNumber());
        var boids = this.model.getBoids();
        for (int i = 0; i < this.numberOfProcessors; i++) {
            final var currentBoids = boids.subList(indexes.get(i), indexes.get(i + 1));
            this.workers.add(() -> {
                try {
                    this.updateBarrier.await();
                    for (final Boid b : currentBoids) {
                        b.updateVelocity(this.model);
                    }
                    this.barrier.await();
                    for (final Boid b : currentBoids) {
                        b.updatePos(this.model);
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public void runSimulation() {
        var executor = Executors.newFixedThreadPool(this.numberOfProcessors);
        while (true) {
            var t0 = System.currentTimeMillis();
            for (final Runnable t : workers) {
                executor.execute(t);
            }

            view.ifPresent(boidsView -> {
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
        this.workers = new ArrayList<>();
        this.model.clearBoids();
    }

    @Override
    public void start() {
        this.isRunning = true;
        initThread();
    }
}
