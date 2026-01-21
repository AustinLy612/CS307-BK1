import java.util.concurrent.ThreadLocalRandom;

public class ObjectOrientedSimulation {
    private static final int DEFAULT_ENTITY_COUNT = 1000;
    private static final double MIN_RADIUS = 0.01;
    private static final double MAX_RADIUS = 0.05;
    private static final double MAX_SPEED = 0.25;
    
    private static final double X_MIN = 0.0;
    private static final double X_MAX = 1.0;
    private static final double Y_MIN = 0.0;
    private static final double Y_MAX = 1.0;

    private final int entityCount;
    private final Circle[] circles;

    public ObjectOrientedSimulation(int entityCount) {
        this.entityCount = entityCount;
        this.circles = new Circle[entityCount];

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

}
