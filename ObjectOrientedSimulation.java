import java.awt.Color;
import java.util.Random;
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

            Color color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            
            circles[i] = new Circle(x, y, vx, vy, color, r, mass);
        }
    }

    public void update(double deltaTime) {
        for (Circle entity : circles) {
            entity.move(deltaTime);
            entity.boundaryBounce();
        }
        CollisionTool.checkCollision(circles);
    }

    public void render() {
        Color lastColor = null;
        for (Circle entity : circles) {
            Color currentColor = entity.getColor();
            if (lastColor == null || !lastColor.equals(currentColor)) {
                StdDraw.setPenColor(currentColor);
                lastColor = currentColor;
            }
            StdDraw.filledCircle(entity.getX(), entity.getY(), entity.getRadius());
        }
    }

    public Circle[] getEntities() {
        return circles;
    }

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
        StdDraw.setXscale(X_MIN, X_MAX);
        StdDraw.setYscale(Y_MIN, Y_MAX);

        ObjectOrientedSimulation system = new ObjectOrientedSimulation(entityCount);

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

    public static class Circle {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private Color color;
        private double radius;
        private double mass;

        public Circle(double x, double y, double vx, double vy, Color color, double radius, double mass) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
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

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

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
