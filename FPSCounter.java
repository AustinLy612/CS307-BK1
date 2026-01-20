public class FPSCounter {
    private long lastTime;
    private int frameCount;
    private static final int WINDOW_SIZE = 60;
    private double[] frameTimes;
    private double sumFrameTime;
    private double currentDeltaTime; // How many seconds have passed in this frame?
    private double avgFrameTime;     // Average frame generation time
    
    public FPSCounter() { //From this frame, count the fps
        this.frameTimes = new double[WINDOW_SIZE];
        start();
    }
    
    public void start() {
        this.lastTime = System.nanoTime();
        this.frameCount = 0;
        this.sumFrameTime = 0.0;
        this.currentDeltaTime = 0.0;
        this.avgFrameTime = 0.0;
        for (int i = 0; i < WINDOW_SIZE; i++) frameTimes[i] = 0.0;
    }
    
    public void update() {
        long currentTime = System.nanoTime();
        // Calculate the Delta Time (in seconds) for this frame
        currentDeltaTime = (currentTime - lastTime) / 1_000_000_000.0;
        lastTime = currentTime;
        
        if (currentDeltaTime <= 0) currentDeltaTime = 0.000001;

        int index = frameCount % WINDOW_SIZE;
        
        if (frameCount >= WINDOW_SIZE) {
            sumFrameTime -= frameTimes[index];
        }
        
        frameTimes[index] = currentDeltaTime;
        sumFrameTime += currentDeltaTime;
        frameCount++;
        
        int actualWindowSize = Math.min(frameCount, WINDOW_SIZE);
        avgFrameTime = sumFrameTime / actualWindowSize;
    }
    
    // Interface

    // Obtain the time difference of the previous frame (in seconds)
    public double getDeltaTime() {
        return currentDeltaTime;
    }
    
    // Obtain the smoothed FPS
    public double getSmoothedFPS() {
        return avgFrameTime > 0 ? 1.0 / avgFrameTime : 0.0;
    }
    
    // Obtain instantaneous FPS
    public double getInstantFPS() {
        return currentDeltaTime > 0 ? 1.0 / currentDeltaTime : 0.0;
    }
    
    // Obtain the average time per frame (in milliseconds)
    public double getAvgFrameTimeMs() {
        return avgFrameTime * 1000.0;
    }
    
    @Override
    public String toString() {
        // Display FPS and milliseconds
        return String.format("FPS: %3.0f | Time: %5.2f ms", 
                            getSmoothedFPS(), getAvgFrameTimeMs());
    }
}
