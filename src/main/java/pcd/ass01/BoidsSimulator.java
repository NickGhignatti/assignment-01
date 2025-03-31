package pcd.ass01;

public interface BoidsSimulator {
    void attachView(final BoidsView view);
    void runSimulation();
    void resumeStop();
    void reset();
    void start();
}
