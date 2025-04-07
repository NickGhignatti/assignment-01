package pcd.ass01;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BoidsTaskExecutorSimulator implements BoidsSimulator {
    private static final int FRAMERATE = 25;

    private int framerate;
    private Boolean isRunning;
    private List<Future> futures;
    private Optional<BoidsView> view;
    private final BoidsModel model;

    public BoidsTaskExecutorSimulator(final BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.view = Optional.empty();
        this.futures = new ArrayList<>();
    }

    @Override
    public void attachView(final BoidsView view) {
        this.view = Optional.of(view);
    }

    @Override
    public void runSimulation() {
        var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        while (true) {
            var t0 = System.currentTimeMillis();

            if (isRunning) {
                for (final Boid b : this.model.getBoids()) {
                    futures.add(executor.submit(() -> b.updateVelocity(this.model)));
                }

                for (final var f : this.futures) {
                    try {
                        f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }

                for (final Boid b : this.model.getBoids()) {
                    futures.add(executor.submit(() -> b.updatePos(this.model)));
                }

                for (final var f : this.futures) {
                    try {
                        f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            view.ifPresent(boidsView -> boidsView.update(framerate));

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
        this.model.clearBoids();
    }

    @Override
    public void start() {
        this.isRunning = true;
    }
}
