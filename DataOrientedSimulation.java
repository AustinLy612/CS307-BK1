public class DataOrientedSimulation {

    private static final double WORLD_MIN = 0.0;
    private static final double WORLD_MAX = 1.0;

    private static final int DEFAULT_ENTITY_COUNT = 1000;

    public static void main(String[] args) {
        int entityCount = DEFAULT_ENTITY_COUNT;
        boolean render = true;

        if (args != null) {
            for (String a : args) {
                if ("--noRender".equalsIgnoreCase(a)) render = false;
                else {
                    try { entityCount = Integer.parseInt(a); } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (render) {
            StdDraw.enableDoubleBuffering();
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setXscale(WORLD_MIN, WORLD_MAX);
            StdDraw.setYscale(WORLD_MIN, WORLD_MAX);
        }

        DataOrientedCircleSystem system = new DataOrientedCircleSystem(entityCount);
        FPSCounter fps = new FPSCounter();

        long lastTime = System.nanoTime();

        long accUpdateNanos = 0L;
        int frames = 0;
        long lastReport = System.nanoTime();

        while (true) {
            long currentTime = System.nanoTime();
            double dt = (currentTime - lastTime) / 1.0e9;
            lastTime = currentTime;

            fps.recordFrame(dt);

            long t0 = System.nanoTime();
            system.update(dt);
            long t1 = System.nanoTime();

            accUpdateNanos += (t1 - t0);
            frames++;

            if (render) {
                StdDraw.clear(StdDraw.BLACK);
                system.render();
                fps.drawOverlayUpperLeft(WORLD_MIN, WORLD_MAX, WORLD_MIN, WORLD_MAX);
                StdDraw.show();
            }

            long now = System.nanoTime();
            if (now - lastReport >= 1_000_000_000L) {
                double avgUpdateMs = (accUpdateNanos / 1_000_000.0) / Math.max(1, frames);
                System.out.printf("Avg update: %.3f ms | FPS(whole): %.0f%n", avgUpdateMs, fps.getSmoothedFPS());
                accUpdateNanos = 0L;
                frames = 0;
                lastReport = now;
            }
        }
    }

    static final class DataOrientedCircleSystem {
        private static final double MIN_RADIUS = 0.01;
        private static final double MAX_RADIUS = 0.05;
        private static final double MAX_SPEED = 0.25;

        private final int entityCount;

        // Structure of Arrays (SoA) layout
        // All data is primitive contiguous arrays.
        private final double[] px; // Position X
        private final double[] py; // Position Y
        private final double[] vx; // Velocity X
        private final double[] vy; // Velocity Y
        private final double[] rad; // Radius
        private final double[] mass; // Mass
        
        // Graphics data separated (hot/cold splitting)
        private final int[] cTopLeft;
        private final int[] cTopRight;
        private final int[] cBottomLeft;
        private final int[] cBottomRight;

        DataOrientedCircleSystem(int entityCount) {
            this.entityCount = entityCount;

            // Allocate flat arrays
            px = new double[entityCount];
            py = new double[entityCount];
            vx = new double[entityCount];
            vy = new double[entityCount];
            rad = new double[entityCount];
            mass = new double[entityCount];
            
            cTopLeft = new int[entityCount];
            cTopRight = new int[entityCount];
            cBottomLeft = new int[entityCount];
            cBottomRight = new int[entityCount];

            initEntities();
        }

        private void initEntities() {
            java.util.Random rand = java.util.concurrent.ThreadLocalRandom.current();
            for (int i = 0; i < entityCount; i++) {
                double r = MIN_RADIUS + rand.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
                rad[i] = r;
                mass[i] = r * r;

                px[i] = rand.nextDouble(WORLD_MIN + r, WORLD_MAX - r);
                py[i] = rand.nextDouble(WORLD_MIN + r, WORLD_MAX - r);

                vx[i] = rand.nextDouble(-MAX_SPEED, MAX_SPEED);
                vy[i] = rand.nextDouble(-MAX_SPEED, MAX_SPEED);

                cTopLeft[i] = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);
                cTopRight[i] = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);
                cBottomLeft[i] = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);
                cBottomRight[i] = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);
            }
        }

        void update(double deltaTime) {
            integrateAndBounce(deltaTime);
        }

        private void integrateAndBounce(double deltaTime) {
            // Tight loop iterating over contiguous arrays.
            // CPU prefetcher will excel here.
            for (int i = 0; i < entityCount; i++) {
                px[i] += vx[i] * deltaTime;
                py[i] += vy[i] * deltaTime;

                double r = rad[i];

                if (px[i] - r < WORLD_MIN) {
                    px[i] = WORLD_MIN + r;
                    vx[i] = -vx[i];
                } else if (px[i] + r > WORLD_MAX) {
                    px[i] = WORLD_MAX - r;
                    vx[i] = -vx[i];
                }

                if (py[i] - r < WORLD_MIN) {
                    py[i] = WORLD_MIN + r;
                    vy[i] = -vy[i];
                } else if (py[i] + r > WORLD_MAX) {
                    py[i] = WORLD_MAX - r;
                    vy[i] = -vy[i];
                }
            }
        }

        void render() {
            for (int i = 0; i < entityCount; i++) {
                StdDraw.filledGradientCircle(
                        px[i], py[i], rad[i],
                        cTopLeft[i], cTopRight[i],
                        cBottomLeft[i], cBottomRight[i]
                );
            }
        }
    }
}
