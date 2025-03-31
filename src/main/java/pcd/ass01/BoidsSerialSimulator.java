package pcd.ass01;

import java.util.Optional;

public class BoidsSerialSimulator implements BoidsSimulator {
    private static final int FRAMERATE = 25;

    private int framerate;
    private boolean isRunning;
    private final BoidsModel model;
    private Optional<BoidsView> view;

    public BoidsSerialSimulator(final BoidsModel model) {
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
                var boids = model.getBoids();
                for (Boid boid : boids) {
                    boid.updateVelocity(model);
                }
                for (Boid boid : boids) {
                    boid.updatePos(model);
                }
            }

    		if (view.isPresent()) {
            	view.get().update(framerate);
            	var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var frameratePeriod = 1000/FRAMERATE;
                
                if (dtElapsed < frameratePeriod) {
                	try {
                		Thread.sleep(frameratePeriod - dtElapsed);
                	} catch (Exception ignored) {}
                	framerate = FRAMERATE;
                } else {
                	framerate = (int) (1000/dtElapsed);
                }
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
