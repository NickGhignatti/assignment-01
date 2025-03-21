package pcd.ass01;

import java.util.Optional;

public class BoidsSerialSimulator implements BoidsSimulator {
    private static final int FRAMERATE = 25;

    private int framerate;
    private boolean isRunning;
    private BoidsModel model;
    private Optional<BoidsView> view;

    public BoidsSerialSimulator(BoidsModel model) {
        this.model = model;
        this.isRunning = false;
        this.view = Optional.empty();
    }

    @Override
    public void attachView(BoidsView view) {
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
                var framratePeriod = 1000/FRAMERATE;
                
                if (dtElapsed < framratePeriod) {		
                	try {
                		Thread.sleep(framratePeriod - dtElapsed);
                	} catch (Exception ignored) {}
                	framerate = FRAMERATE;
                } else {
                	framerate = (int) (1000/dtElapsed);
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
