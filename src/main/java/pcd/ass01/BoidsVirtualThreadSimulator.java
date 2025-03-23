package pcd.ass01;

import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BoidsVirtualThreadSimulator implements BoidsSimulator{
    private static final int FRAMERATE = 25;

    private int framerate;
    private Boolean isRunning;
    private CyclicBarrier barrier;
    private Optional<BoidsView> view;
    private final BoidsModel model;

    public BoidsVirtualThreadSimulator(final BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.view = Optional.empty();
    }

    @Override
    public void attachView(final BoidsView view) {
        this.view = Optional.of(view);
    }

    @Override
    public void runSimulation() {
        while (true) {
            var t0 = System.currentTimeMillis();
            if (this.isRunning) {
                this.barrier = new CyclicBarrier(this.model.getBoidsNumber());
                var boids = model.getBoids();
                for (final Boid b: boids) {
                    Thread.startVirtualThread(() -> {
                        b.updateVelocity(this.model);
                        try {
                            barrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }
                        b.updatePos(this.model);
                    });
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
