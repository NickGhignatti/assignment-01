package pcd.ass01;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoidsVirtualThreadSimulator implements BoidsSimulator {
    private static final int FRAMERATE = 25;

    private int framerate;
    private List<Boid> boids;
    private Boolean isRunning;
    private Boolean resetThread;
    private BoidsBarrier barrier;
    private Optional<BoidsView> view;
    private BoidsBarrier updateBarrier;
    private final BoidsModel model;

    public BoidsVirtualThreadSimulator(final BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.view = Optional.empty();
        this.boids = new ArrayList<>();
        this.resetThread = false;
    }

    @Override
    public void attachView(final BoidsView view) {
        this.view = Optional.of(view);
    }

    @Override
    public void runSimulation() {
        while (true) {
            var t0 = System.currentTimeMillis();
            this.view.ifPresent(boidsView -> {
                if (this.isRunning) {
                    try {
                        this.updateBarrier.await();
                    } catch (InterruptedException e) {
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
    public void resumeStop() {
        this.isRunning = !this.isRunning;
    }

    @Override
    public void reset() {
        this.isRunning = false;

        this.updateBarrier.breakBarrier();

        this.resetThread = true;
        this.model.clearBoids();
    }

    @Override
    public void start() {
        this.isRunning = true;
        this.resetThread = false;
        this.barrier = new BoidsBarrier(this.model.getBoidsNumber());
        this.updateBarrier = new BoidsBarrier(this.model.getBoidsNumber() + 1);
        boids = model.getBoids();
        for (final Boid b : boids) {
            Thread.startVirtualThread(() -> {
                while (!this.resetThread) {
                    try {
                        if (this.view.isPresent()) this.updateBarrier.await();
                        b.updateVelocity(this.model);
                        this.barrier.await();
                        b.updatePos(this.model);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }
}
