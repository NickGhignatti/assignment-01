package pcd.ass01;

public interface BoidsSimulator {
    void attachView(final BoidsView view);
    void runSimulation();
    void resume();
    void stop();
}
