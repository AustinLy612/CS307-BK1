public class DataOrientedSimulation {

    private static final double WORLD_MIN = 0.0;
    private static final double WORLD_MAX = 1.0;

    private static final int DEFAULT_ENTITY_COUNT = 20000;

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
        private static final double MIN_RADIUS = 0.001;
        private static final double MAX_RADIUS = 0.005;
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

        // Grid system
        private final double cellSize;
        private final int gridWidth;
        private final int gridHeight;
        private final int[] cellHead;
        private final int[] nextInCell;

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

            cellSize = 2.0 * MAX_RADIUS;
            gridWidth = Math.max(1, (int) Math.ceil((WORLD_MAX - WORLD_MIN) / cellSize));
            gridHeight = gridWidth;

            cellHead = new int[gridWidth * gridHeight];
            nextInCell = new int[entityCount];
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
            rebuildGrid();
            resolveCollisions();
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

        private void rebuildGrid() {
            java.util.Arrays.fill(cellHead, -1);
            for (int i = 0; i < entityCount; i++) {
                int cx = (int) ((px[i] - WORLD_MIN) / cellSize);
                int cy = (int) ((py[i] - WORLD_MIN) / cellSize);

                if (cx < 0) cx = 0;
                else if (cx >= gridWidth) cx = gridWidth - 1;

                if (cy < 0) cy = 0;
                else if (cy >= gridHeight) cy = gridHeight - 1;

                int cellIndex = cx + cy * gridWidth;
                nextInCell[i] = cellHead[cellIndex];
                cellHead[cellIndex] = i;
            }
        }

        private void resolveCollisions() {
            // Collision resolution logic remains essentially the same,
            // but now accesses data via indices.
            for (int cy = 0; cy < gridHeight; cy++) {
                for (int cx = 0; cx < gridWidth; cx++) {
                    int cellIndex = cx + cy * gridWidth;
                    
                    // Same cell
                    int i = cellHead[cellIndex];
                    while (i != -1) {
                        int j = nextInCell[i];
                        while (j != -1) {
                            resolvePair(i, j);
                            j = nextInCell[j];
                        }
                        i = nextInCell[i];
                    }

                    // Neighbor cells
                    if (cx + 1 < gridWidth) resolveCellPairAcross(cellIndex, (cx + 1) + cy * gridWidth);
                    if (cy + 1 < gridHeight) resolveCellPairAcross(cellIndex, cx + (cy + 1) * gridWidth);
                    if (cx + 1 < gridWidth && cy + 1 < gridHeight) {
                        resolveCellPairAcross(cellIndex, (cx + 1) + (cy + 1) * gridWidth);
                    }
                    if (cx - 1 >= 0 && cy + 1 < gridHeight) {
                        resolveCellPairAcross(cellIndex, (cx - 1) + (cy + 1) * gridWidth);
                    }
                }
            }
        }

        private void resolveCellPairAcross(int cellA, int cellB) {
            int i = cellHead[cellA];
            while (i != -1) {
                int j = cellHead[cellB];
                while (j != -1) {
                    resolvePair(i, j);
                    j = nextInCell[j];
                }
                i = nextInCell[i];
            }
        }

        private void resolvePair(int i, int j) {
            double dx = px[j] - px[i];
            double dy = py[j] - py[i];
            double dist2 = dx * dx + dy * dy;

            double rSum = rad[i] + rad[j];
            double rSum2 = rSum * rSum;

            if (dist2 >= rSum2 || dist2 <= 0.0) return;

            double dist = Math.sqrt(dist2);
            double nx = dx / dist;
            double ny = dy / dist;

            double m1 = mass[i];
            double m2 = mass[j];

            double overlap = rSum - dist;
            double moveFactor = overlap / (m1 + m2);

            // Direct array writes
            double m2nx = m2 * nx; 
            double m2ny = m2 * ny;
            double m1nx = m1 * nx;
            double m1ny = m1 * ny;

            px[i] -= moveFactor * m2nx;
            py[i] -= moveFactor * m2ny;
            px[j] += moveFactor * m1nx;
            py[j] += moveFactor * m1ny;

            double dvx = vx[i] - vx[j];
            double dvy = vy[i] - vy[j];
            double vn = dvx * nx + dvy * ny;

            if (vn > 0.0) {
                double impulse = (2.0 * vn) / (m1 + m2);
                vx[i] -= impulse * m2nx;
                vy[i] -= impulse * m2ny;
                vx[j] += impulse * m1nx;
                vy[j] += impulse * m1ny;
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
