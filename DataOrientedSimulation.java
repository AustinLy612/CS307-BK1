public class DataOrientedSimulation {

    private static final double WORLD_MIN = 0.0;
    private static final double WORLD_MAX = 1.0;

    private static final int DEFAULT_ENTITY_COUNT = 20000;

    public static void main(String[] args) {
        int entityCount = DEFAULT_ENTITY_COUNT;
        if (args != null && args.length > 0) {
            try {
                entityCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        StdDraw.enableDoubleBuffering();
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setXscale(WORLD_MIN, WORLD_MAX);
        StdDraw.setYscale(WORLD_MIN, WORLD_MAX);

        DataOrientedCircleSystem system = new DataOrientedCircleSystem(entityCount);

        long lastTime = System.nanoTime();
        while (true) {
            long currentTime = System.nanoTime();
            double dt = (currentTime - lastTime) / 1.0e9;
            lastTime = currentTime;

            system.update(dt);

            StdDraw.clear(StdDraw.BLACK);
            system.render();
            StdDraw.show();
        }
    }

    static final class DataOrientedCircleSystem {
        private static final double MIN_RADIUS = 0.001;
        private static final double MAX_RADIUS = 0.005;
        private static final double MAX_SPEED = 0.25;

        private final int entityCount;

        private final double[] x;
        private final double[] y;
        private final double[] vx;
        private final double[] vy;
        private final double[] radius;
        private final double[] mass;
        private final int[] colorR;
        private final int[] colorG;
        private final int[] colorB;

        private final double cellSize;
        private final int gridWidth;
        private final int gridHeight;
        private final int[] cellHead;
        private final int[] nextInCell;

        DataOrientedCircleSystem(int entityCount) {
            this.entityCount = entityCount;

            x = new double[entityCount];
            y = new double[entityCount];
            vx = new double[entityCount];
            vy = new double[entityCount];
            radius = new double[entityCount];
            mass = new double[entityCount];
            colorR = new int[entityCount];
            colorG = new int[entityCount];
            colorB = new int[entityCount];

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
                radius[i] = r;
                mass[i] = r * r;

                x[i] = rand.nextDouble(WORLD_MIN + r, WORLD_MAX - r);
                y[i] = rand.nextDouble(WORLD_MIN + r, WORLD_MAX - r);

                vx[i] = rand.nextDouble(-MAX_SPEED, MAX_SPEED);
                vy[i] = rand.nextDouble(-MAX_SPEED, MAX_SPEED);

                colorR[i] = rand.nextInt(256);
                colorG[i] = rand.nextInt(256);
                colorB[i] = rand.nextInt(256);
            }
        }

        void update(double deltaTime) {
            integrateAndBounce(deltaTime);
            rebuildGrid();
            resolveCollisions();
        }

        private void integrateAndBounce(double deltaTime) {
            for (int i = 0; i < entityCount; i++) {
                x[i] += vx[i] * deltaTime;
                y[i] += vy[i] * deltaTime;

                double r = radius[i];

                if (x[i] - r < WORLD_MIN) {
                    x[i] = WORLD_MIN + r;
                    vx[i] = -vx[i];
                } else if (x[i] + r > WORLD_MAX) {
                    x[i] = WORLD_MAX - r;
                    vx[i] = -vx[i];
                }

                if (y[i] - r < WORLD_MIN) {
                    y[i] = WORLD_MIN + r;
                    vy[i] = -vy[i];
                } else if (y[i] + r > WORLD_MAX) {
                    y[i] = WORLD_MAX - r;
                    vy[i] = -vy[i];
                }
            }
        }

        private void rebuildGrid() {
            java.util.Arrays.fill(cellHead, -1);
            for (int i = 0; i < entityCount; i++) {
                int cx = (int) ((x[i] - WORLD_MIN) / cellSize);
                int cy = (int) ((y[i] - WORLD_MIN) / cellSize);

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
            for (int cy = 0; cy < gridHeight; cy++) {
                for (int cx = 0; cx < gridWidth; cx++) {
                    int cellIndex = cx + cy * gridWidth;
                    resolveCellPairs(cellIndex);

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

        private void resolveCellPairs(int cellIndex) {
            for (int i = cellHead[cellIndex]; i != -1; i = nextInCell[i]) {
                for (int j = nextInCell[i]; j != -1; j = nextInCell[j]) {
                    resolvePair(i, j);
                }
            }
        }

        private void resolveCellPairAcross(int cellA, int cellB) {
            for (int i = cellHead[cellA]; i != -1; i = nextInCell[i]) {
                for (int j = cellHead[cellB]; j != -1; j = nextInCell[j]) {
                    resolvePair(i, j);
                }
            }
        }

        private void resolvePair(int i, int j) {
            double dx = x[j] - x[i];
            double dy = y[j] - y[i];
            double dist2 = dx * dx + dy * dy;

            double rSum = radius[i] + radius[j];
            double rSum2 = rSum * rSum;

            if (dist2 >= rSum2 || dist2 <= 0.0) return;

            double dist = Math.sqrt(dist2);
            double nx = dx / dist;
            double ny = dy / dist;

            double m1 = mass[i];
            double m2 = mass[j];

            double overlap = rSum - dist;
            double moveFactor = overlap / (m1 + m2);

            x[i] -= moveFactor * m2 * nx;
            y[i] -= moveFactor * m2 * ny;
            x[j] += moveFactor * m1 * nx;
            y[j] += moveFactor * m1 * ny;

            double dvx = vx[i] - vx[j];
            double dvy = vy[i] - vy[j];
            double vn = dvx * nx + dvy * ny;

            if (vn > 0.0) {
                double impulse = (2.0 * vn) / (m1 + m2);
                vx[i] -= impulse * m2 * nx;
                vy[i] -= impulse * m2 * ny;
                vx[j] += impulse * m1 * nx;
                vy[j] += impulse * m1 * ny;
            }
        }

        void render() {
            int lastR = -1, lastG = -1, lastB = -1;
            for (int i = 0; i < entityCount; i++) {
                int r = colorR[i];
                int g = colorG[i];
                int b = colorB[i];
                if (r != lastR || g != lastG || b != lastB) {
                    StdDraw.setPenColor(r, g, b);
                    lastR = r;
                    lastG = g;
                    lastB = b;
                }
                StdDraw.filledCircle(x[i], y[i], radius[i]);
            }
        }
    }
}
