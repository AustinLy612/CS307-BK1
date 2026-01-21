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
        FPSCounter fps = new FPSCounter();

        long lastTime = System.nanoTime();
        while (true) {
            long currentTime = System.nanoTime();
            double dt = (currentTime - lastTime) / 1.0e9;
            lastTime = currentTime;

            fps.recordFrame(dt);

            system.update(dt);

            StdDraw.clear(StdDraw.BLACK);
            system.render();
            fps.drawOverlayUpperLeft(WORLD_MIN, WORLD_MAX, WORLD_MIN, WORLD_MAX);
            StdDraw.show();
        }
    }

    static final class DataOrientedCircleSystem {
        static final class PhysicsComponent {
            double x, y;
            double vx, vy;
            double radius;
            double mass;
        }

        static final class GraphicsComponent {
            int r, g, b;
        }

        private static final double MIN_RADIUS = 0.001;
        private static final double MAX_RADIUS = 0.005;
        private static final double MAX_SPEED = 0.25;

        private final int entityCount;

        private final PhysicsComponent[] physics;
        private final GraphicsComponent[] graphics;

        private final double cellSize;
        private final int gridWidth;
        private final int gridHeight;
        private final int[] cellHead;
        private final int[] nextInCell;

        DataOrientedCircleSystem(int entityCount) {
            this.entityCount = entityCount;

            physics = new PhysicsComponent[entityCount];
            graphics = new GraphicsComponent[entityCount];
            
            for (int i = 0; i < entityCount; i++) {
                physics[i] = new PhysicsComponent();
                graphics[i] = new GraphicsComponent();
            }

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
                PhysicsComponent p = physics[i];
                GraphicsComponent g = graphics[i];

                double r = MIN_RADIUS + rand.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
                p.radius = r;
                p.mass = r * r;

                p.x = rand.nextDouble(WORLD_MIN + r, WORLD_MAX - r);
                p.y = rand.nextDouble(WORLD_MIN + r, WORLD_MAX - r);

                p.vx = rand.nextDouble(-MAX_SPEED, MAX_SPEED);
                p.vy = rand.nextDouble(-MAX_SPEED, MAX_SPEED);

                g.r = rand.nextInt(256);
                g.g = rand.nextInt(256);
                g.b = rand.nextInt(256);
            }
        }

        void update(double deltaTime) {
            integrateAndBounce(deltaTime);
            rebuildGrid();
            resolveCollisions();
        }

        private void integrateAndBounce(double deltaTime) {
            for (int i = 0; i < entityCount; i++) {
                PhysicsComponent p = physics[i];
                p.x += p.vx * deltaTime;
                p.y += p.vy * deltaTime;

                double r = p.radius;

                if (p.x - r < WORLD_MIN) {
                    p.x = WORLD_MIN + r;
                    p.vx = -p.vx;
                } else if (p.x + r > WORLD_MAX) {
                    p.x = WORLD_MAX - r;
                    p.vx = -p.vx;
                }

                if (p.y - r < WORLD_MIN) {
                    p.y = WORLD_MIN + r;
                    p.vy = -p.vy;
                } else if (p.y + r > WORLD_MAX) {
                    p.y = WORLD_MAX - r;
                    p.vy = -p.vy;
                }
            }
        }

        private void rebuildGrid() {
            java.util.Arrays.fill(cellHead, -1);
            for (int i = 0; i < entityCount; i++) {
                PhysicsComponent p = physics[i];
                int cx = (int) ((p.x - WORLD_MIN) / cellSize);
                int cy = (int) ((p.y - WORLD_MIN) / cellSize);

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
            PhysicsComponent p1 = physics[i];
            PhysicsComponent p2 = physics[j];

            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double dist2 = dx * dx + dy * dy;

            double rSum = p1.radius + p2.radius;
            double rSum2 = rSum * rSum;

            if (dist2 >= rSum2 || dist2 <= 0.0) return;

            double dist = Math.sqrt(dist2);
            double nx = dx / dist;
            double ny = dy / dist;

            double m1 = p1.mass;
            double m2 = p2.mass;

            double overlap = rSum - dist;
            double moveFactor = overlap / (m1 + m2);

            p1.x -= moveFactor * m2 * nx;
            p1.y -= moveFactor * m2 * ny;
            p2.x += moveFactor * m1 * nx;
            p2.y += moveFactor * m1 * ny;

            double dvx = p1.vx - p2.vx;
            double dvy = p1.vy - p2.vy;
            double vn = dvx * nx + dvy * ny;

            if (vn > 0.0) {
                double impulse = (2.0 * vn) / (m1 + m2);
                p1.vx -= impulse * m2 * nx;
                p1.vy -= impulse * m2 * ny;
                p2.vx += impulse * m1 * nx;
                p2.vy += impulse * m1 * ny;
            }
        }

        void render() {
            int lastR = -1, lastG = -1, lastB = -1;
            for (int i = 0; i < entityCount; i++) {
                GraphicsComponent g = graphics[i];
                if (g.r != lastR || g.g != lastG || g.b != lastB) {
                    StdDraw.setPenColor(g.r, g.g, g.b);
                    lastR = g.r;
                    lastG = g.g;
                    lastB = g.b;
                }
                PhysicsComponent p = physics[i];
                StdDraw.filledCircle(p.x, p.y, p.radius);
            }
        }
    }
}
