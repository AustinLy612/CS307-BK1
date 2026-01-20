import java.awt.Color;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class OOP_CircleSystem {
    private static final double CIRCLE_RADIUS = 0.05;
    private static final double MAX_SPEED = 0.1;
    private static final double X_MIN = -5.0;
    private static final double X_MAX = 5.0;
    private static final double Y_MIN = -5.0;
    private static final double Y_MAX = 5.0;

    private final int entityCount;
    private final OOP_Entity[] oopEntities;

    public OOP_CircleSystem(int entityCount) {
        this.entityCount = entityCount;
        this.oopEntities = new OOP_Entity[entityCount];
        initAllEntities();
    }

    private void initAllEntities() {
        Random rand = ThreadLocalRandom.current();
        for (int i = 0; i < entityCount; i++) {
            double x = rand.nextDouble(X_MIN, X_MAX);
            double y = rand.nextDouble(Y_MIN, Y_MAX);
            double vx = rand.nextDouble(-MAX_SPEED, MAX_SPEED);
            double vy = rand.nextDouble(-MAX_SPEED, MAX_SPEED);
            Color color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            oopEntities[i] = new OOP_Entity(x, y, vx, vy, color, CIRCLE_RADIUS, 1.0);
        }
    }

    public void update(double deltaTime) {
        for (OOP_Entity entity : oopEntities) {
            entity.move(deltaTime);
            entity.boundaryBounce();
        }
        CollisionTool.checkCollision(oopEntities);
    }

    public void render() {
        Color lastColor = null;
        for (OOP_Entity entity : oopEntities) {
            Color currentColor = entity.getColor();
            if (lastColor == null || !lastColor.equals(currentColor)) {
                StdDraw.setPenColor(currentColor);
                lastColor = currentColor;
            }
            StdDraw.filledCircle(entity.getX(), entity.getY(), entity.getRadius());
        }
    }

    public OOP_Entity[] getEntities() {
        return oopEntities;
    }

    public static class OOP_Entity {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private Color color;
        private double radius;
        private double mass;

        public OOP_Entity(double x, double y, double vx, double vy, Color color, double radius, double mass) {
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
        public static void checkCollision(OOP_Entity[] entities) {
            // TODO: Replace with D's collision logic.
        }
    }
}
