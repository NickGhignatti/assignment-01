package pcd.ass01;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BoidsPlatformThreadSimulator implements BoidsSimulator{
    private static final int FRAMERATE = 25;

    private int framerate;
    private List<Boid> boids;
    private Boolean isRunning;
    private Boolean isFirstTime;
    private Optional<BoidsView> view;
    private final BoidsModel model;
    private final CyclicBarrier barrier;
    private final int numberOfProcessors;

    public BoidsPlatformThreadSimulator(final BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.isFirstTime = true;
        this.view = Optional.empty();
        this.boids = new ArrayList<>();
        this.numberOfProcessors = Runtime.getRuntime().availableProcessors() + 1;
        this.barrier = new CyclicBarrier(this.numberOfProcessors);
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
            if (this.isRunning) {
                if (this.isFirstTime) {
                    this.isFirstTime = false;
                    int step = this.model.getBoidsNumber() / this.numberOfProcessors;
                    for (int i = 0; i < this.numberOfProcessors; i++) {
                        indexes.add(i * step);
                    }
                    indexes.add(this.model.getBoidsNumber());
                    boids = this.model.getBoids();
                }
                for (int i = 0; i < this.numberOfProcessors; i++) {
                    final int finalI = i;
                    new Thread(() -> {
                        for (final Boid b: boids.subList(indexes.get(finalI), indexes.get(finalI + 1))) {
                            try {
                                b.updateVelocity(this.model);
                                this.barrier.await();
                                b.updatePos(this.model);
                            } catch (InterruptedException | BrokenBarrierException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }).start();
                }
            }

            if (view.isPresent()) {
                view.get().update(framerate);
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
