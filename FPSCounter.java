import java.awt.Color;
import java.awt.Font;

/**
 * FPS (frames-per-second) counter with simple smoothing.
 *
 * Usage patterns:
 *  - If you already compute dt: call {@link #recordFrame(double)} each frame.
 *  - If you don't compute dt: call {@link #tick()} each frame to measure it.
 */
public final class FPSCounter {
    private static final int DEFAULT_WINDOW_SIZE = 60;
    private static final double MIN_DT_SECONDS = 1.0e-9;

    private final int windowSize;
    private final double[] frameTimes;

    private int cursor;
    private int samples;
    private double sumFrameTime;
    private double lastDeltaTimeSeconds;

    private long lastTickNanos;
    private boolean hasLastTick;

    private Font overlayFont;
    private Color overlayColor;

    public FPSCounter() {
        this(DEFAULT_WINDOW_SIZE);
    }

    public FPSCounter(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        this.frameTimes = new double[windowSize];
        this.overlayFont = new Font("SansSerif", Font.PLAIN, 16);
        this.overlayColor = StdDraw.WHITE;
        reset();
    }

    /** Resets smoothing state and internal timer. */
    public void reset() {
        this.cursor = 0;
        this.samples = 0;
        this.sumFrameTime = 0.0;
        this.lastDeltaTimeSeconds = 0.0;
        for (int i = 0; i < windowSize; i++) {
            frameTimes[i] = 0.0;
        }
        this.lastTickNanos = System.nanoTime();
        this.hasLastTick = false;
    }

    /**
     * Records a frame using a caller-provided delta time in seconds.
     * This is the preferred path when your simulation already computes dt.
     */
    public void recordFrame(double deltaTimeSeconds) {
        if (!Double.isFinite(deltaTimeSeconds) || deltaTimeSeconds <= 0.0) {
            deltaTimeSeconds = MIN_DT_SECONDS;
        }

        lastDeltaTimeSeconds = deltaTimeSeconds;

        if (samples < windowSize) {
            frameTimes[cursor] = deltaTimeSeconds;
            sumFrameTime += deltaTimeSeconds;
            samples++;
        } else {
            sumFrameTime -= frameTimes[cursor];
            frameTimes[cursor] = deltaTimeSeconds;
            sumFrameTime += deltaTimeSeconds;
        }

        cursor++;
        if (cursor >= windowSize) {
            cursor = 0;
        }
    }

    /** Measures dt using {@link System#nanoTime()}, records it, and returns it. */
    public double tick() {
        long now = System.nanoTime();
        if (!hasLastTick) {
            lastTickNanos = now;
            hasLastTick = true;
            recordFrame(MIN_DT_SECONDS);
            return MIN_DT_SECONDS;
        }

        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        recordFrame(dt);
        return lastDeltaTimeSeconds;
    }

    /** Last recorded delta time (seconds). */
    public double getDeltaTimeSeconds() {
        return lastDeltaTimeSeconds;
    }

    /** Average frame time over the smoothing window (seconds). */
    public double getAvgFrameTimeSeconds() {
        if (samples == 0) return 0.0;
        return sumFrameTime / samples;
    }

    /** Average frame time over the smoothing window (milliseconds). */
    public double getAvgFrameTimeMs() {
        return getAvgFrameTimeSeconds() * 1000.0;
    }

    /** Smoothed FPS computed from the smoothed average frame time. */
    public double getSmoothedFPS() {
        double avg = getAvgFrameTimeSeconds();
        return avg > 0.0 ? 1.0 / avg : 0.0;
    }

    /** Instantaneous FPS computed from the last recorded frame time. */
    public double getInstantFPS() {
        return lastDeltaTimeSeconds > 0.0 ? 1.0 / lastDeltaTimeSeconds : 0.0;
    }

    public int getWindowSize() {
        return windowSize;
    }

    /** Controls how the overlay is rendered (defaults: 16pt SansSerif, white). */
    public void setOverlayStyle(Font font, Color color) {
        if (font != null) this.overlayFont = font;
        if (color != null) this.overlayColor = color;
    }

    /** Returns a short human-friendly HUD string. */
    public String getOverlayText() {
        return String.format("FPS: %.0f", getSmoothedFPS());
    }

    /**
     * Draws the FPS HUD text left-aligned at (x,y). Call after your scene render.
     * Preserves the previous StdDraw pen color and font.
     */
    public void drawOverlay(double x, double y) {
        Color prevColor = StdDraw.getPenColor();
        Font prevFont = StdDraw.getFont();

        StdDraw.setPenColor(overlayColor);
        StdDraw.setFont(overlayFont);
        StdDraw.textLeft(x, y, getOverlayText());

        StdDraw.setFont(prevFont);
        StdDraw.setPenColor(prevColor);
    }

    /** Convenience helper for "upper-left" in a world-scaled coordinate system. */
    public void drawOverlayUpperLeft(double xMin, double xMax, double yMin, double yMax) {
        double padX = 0.01 * (xMax - xMin);
        double padY = 0.02 * (yMax - yMin);
        drawOverlay(xMin + padX, yMax - padY);
    }

    @Override
    public String toString() {
        return String.format("FPS: %.0f | Time: %.2f ms", getSmoothedFPS(), getAvgFrameTimeMs());
    }
}
