import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class ObjectOrientedSimulation {
    private static final int DEFAULT_ENTITY_COUNT = 20000;
    private static final double MIN_RADIUS = 0.001;
    private static final double MAX_RADIUS = 0.005;
    private static final double MAX_SPEED = 0.25;
    
    private static final double X_MIN = 0.0;
    private static final double X_MAX = 1.0;
    private static final double Y_MIN = 0.0;
    private static final double Y_MAX = 1.0;

    private final int entityCount;
    private final Circle[] circles;

    // Grid collision fields
    private final double cellSize;
    private final int gridWidth;
    private final int gridHeight;
    private final int[] cellHead;
    private final int[] nextInCell;

    public ObjectOrientedSimulation(int entityCount) {
        this.entityCount = entityCount;
        this.circles = new Circle[entityCount];
        
        // Initialize grid dimensions
        this.cellSize = 2.0 * MAX_RADIUS;
        this.gridWidth = Math.max(1, (int) Math.ceil((X_MAX - X_MIN) / cellSize));
        this.gridHeight = Math.max(1, (int) Math.ceil((Y_MAX - Y_MIN) / cellSize));
        
        this.cellHead = new int[gridWidth * gridHeight];
        this.nextInCell = new int[entityCount];

        initAllEntities();
    }

    private void initAllEntities() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < entityCount; i++) {
            double r = MIN_RADIUS + rand.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
            double mass = r * r;

            double x = rand.nextDouble(X_MIN + r, X_MAX - r);
            double y = rand.nextDouble(Y_MIN + r, Y_MAX - r);

            double vx = rand.nextDouble(-MAX_SPEED, MAX_SPEED);
            double vy = rand.nextDouble(-MAX_SPEED, MAX_SPEED);

            int topLeft = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);
            int topRight = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);
            int bottomLeft = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);
            int bottomRight = (rand.nextInt(256) << 16) | (rand.nextInt(256) << 8) | rand.nextInt(256);

            circles[i] = new Circle(x, y, vx, vy, topLeft, topRight, bottomLeft, bottomRight, r, mass);
        }
    }

    public void update(double deltaTime) {
        for (Circle entity : circles) {
            entity.move(deltaTime);
            entity.boundaryBounce();
        }
        // CollisionTool.checkCollision(circles); // Removed brute force
        rebuildGrid();
        resolveCollisions();
    }

    private void rebuildGrid() {
        Arrays.fill(cellHead, -1);
        for (int i = 0; i < entityCount; i++) {
            Circle c = circles[i];
            int cx = (int) ((c.x - X_MIN) / cellSize);
            int cy = (int) ((c.y - Y_MIN) / cellSize);

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
        Circle c1 = circles[i];
        Circle c2 = circles[j];

        double dx = c2.x - c1.x;
        double dy = c2.y - c1.y;
        double dist2 = dx * dx + dy * dy;

        double rSum = c1.radius + c2.radius;
        double rSum2 = rSum * rSum;

        if (dist2 >= rSum2 || dist2 <= 0.0) return;

        double dist = Math.sqrt(dist2);
        double nx = dx / dist;
        double ny = dy / dist;

        double m1 = c1.mass;
        double m2 = c2.mass;

        // Position correction (overlap)
        double overlap = rSum - dist;
        double moveFactor = overlap / (m1 + m2);

        c1.x -= moveFactor * m2 * nx;
        c1.y -= moveFactor * m2 * ny;
        c2.x += moveFactor * m1 * nx;
        c2.y += moveFactor * m1 * ny;

        // Velocity resolution
        double dvx = c1.vx - c2.vx;
        double dvy = c1.vy - c2.vy;
        double vn = dvx * nx + dvy * ny;

        if (vn > 0.0) {
            double impulse = (2.0 * vn) / (m1 + m2);
            c1.vx -= impulse * m2 * nx;
            c1.vy -= impulse * m2 * ny;
            c2.vx += impulse * m1 * nx;
            c2.vy += impulse * m1 * ny;
        }
    }

    public void render() {
        for (Circle entity : circles) {
            StdDraw.filledGradientCircle(
                    entity.getX(), entity.getY(), entity.getRadius(),
                    entity.getTopLeftRGB(), entity.getTopRightRGB(),
                    entity.getBottomLeftRGB(), entity.getBottomRightRGB()
            );
        }
    }

    public Circle[] getEntities() {
        return circles;
    }

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
            StdDraw.setXscale(X_MIN, X_MAX);
            StdDraw.setYscale(Y_MIN, Y_MAX);
        }

        ObjectOrientedSimulation system = new ObjectOrientedSimulation(entityCount);
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
                fps.drawOverlayUpperLeft(X_MIN, X_MAX, Y_MIN, Y_MAX);
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

    public static class Circle {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private int topLeftRGB;
        private int topRightRGB;
        private int bottomLeftRGB;
        private int bottomRightRGB;
        private double radius;
        private double mass;

        public Circle(double x, double y, double vx, double vy,
                      int topLeftRGB, int topRightRGB, int bottomLeftRGB, int bottomRightRGB,
                      double radius, double mass) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.topLeftRGB = topLeftRGB;
            this.topRightRGB = topRightRGB;
            this.bottomLeftRGB = bottomLeftRGB;
            this.bottomRightRGB = bottomRightRGB;
            this.radius = radius;
            this.mass = mass;
        }

        public void move(double deltaTime) {
            x += vx * deltaTime;
            y += vy * deltaTime;
        }

        public void boundaryBounce() {
            if (x - radius < X_MIN) {
                x = X_MIN + radius;
                vx = -vx;
            } else if (x + radius > X_MAX) {
                x = X_MAX - radius;
                vx = -vx;
            }

            if (y - radius < Y_MIN) {
                y = Y_MIN + radius;
                vy = -vy;
            } else if (y + radius > Y_MAX) {
                y = Y_MAX - radius;
                vy = -vy;
            }
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getVx() {
            return vx;
        }

        public void setVx(double vx) {
            this.vx = vx;
        }

        public double getVy() {
            return vy;
        }

        public void setVy(double vy) {
            this.vy = vy;
        }

        public int getTopLeftRGB() { return topLeftRGB; }

        public void setTopLeftRGB(int topLeftRGB) { this.topLeftRGB = topLeftRGB; }

        public int getTopRightRGB() { return topRightRGB; }

        public void setTopRightRGB(int topRightRGB) { this.topRightRGB = topRightRGB; }

        public int getBottomLeftRGB() { return bottomLeftRGB; }

        public void setBottomLeftRGB(int bottomLeftRGB) { this.bottomLeftRGB = bottomLeftRGB; }

        public int getBottomRightRGB() { return bottomRightRGB; }

        public void setBottomRightRGB(int bottomRightRGB) { this.bottomRightRGB = bottomRightRGB; }

        public double getRadius() {
            return radius;
        }

        public void setRadius(double radius) {
            this.radius = radius;
        }

        public double getMass() {
            return mass;
        }

        public void setMass(double mass) {
            this.mass = mass;
        }
    }

    public static class CollisionTool {
        public static void checkCollision(Circle[] circles) {
            for (int i = 0; i < circles.length; i++) {
                for (int j = i + 1; j < circles.length; j++) {
                    Circle c1 = circles[i];
                    Circle c2 = circles[j];
                    double dx = c2.x - c1.x;
                    double dy = c2.y - c1.y;
                    double distSq = dx * dx + dy * dy;
                    double radSum = c1.radius + c2.radius;
                    
                    if (distSq < radSum * radSum) {
                        double distance = Math.sqrt(distSq);
                        double nx = dx / distance;
                        double ny = dy / distance;
                        
                        double dvx = c2.vx - c1.vx;
                        double dvy = c2.vy - c1.vy;
                        
                        double dotProduct = dvx * nx + dvy * ny;
                        
                        if (dotProduct < 0) {
                            double massSum = c1.mass + c2.mass;
                            
                            double impulse = (2 * dotProduct) / massSum;
                            
                            c1.vx += impulse * c2.mass * nx;
                            c1.vy += impulse * c2.mass * ny;
                            c2.vx -= impulse * c1.mass * nx;
                            c2.vy -= impulse * c1.mass * ny;
                        }
                    }
                }
            }
        }
    }
}
