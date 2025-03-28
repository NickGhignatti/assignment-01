package pcd.ass01;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BoidsVirtualThreadSimulator implements BoidsSimulator {
    private static final int FRAMERATE = 25;

    private int framerate;
    private List<Boid> boids;
    private Boolean isRunning;
    private Boolean isFirstTime;
    private CyclicBarrier barrier;
    private Optional<BoidsView> view;
    private CyclicBarrier updateBarrier;
    private final BoidsModel model;

    public BoidsVirtualThreadSimulator(final BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.isFirstTime = true;
        this.view = Optional.empty();
        this.boids = new ArrayList<>();
    }

    @Override
    public void attachView(final BoidsView view) {
        this.view = Optional.of(view);
    }

    @Override
    public void runSimulation() {
        while (true) {
            var t0 = System.currentTimeMillis();
            if (this.isFirstTime && this.isRunning) {
                this.isFirstTime = false;
                this.barrier = new CyclicBarrier(this.model.getBoidsNumber());
                this.updateBarrier = new CyclicBarrier(this.model.getBoidsNumber() + 1);
                boids = model.getBoids();
                for (final Boid b : boids) {
                    Thread.startVirtualThread(() -> {
                        while (true) {
                            try {
                                this.updateBarrier.await();
                                b.updateVelocity(this.model);
                                barrier.await();
                                b.updatePos(this.model);
                            } catch (InterruptedException | BrokenBarrierException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
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

            var t1 = System.currentTimeMillis();
            var dtElapsed = t1 - t0;
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
    public void resume() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.isRunning = false;
    }
}
