import java.awt.Color;

public class Simulation {

    static class Circle {
        float[] color; // RGB color
        float r;       // radius
        float[] p;     // position
        float[] v;     // velocity

        public Circle() {
            color = new float[3];
            p = new float[2];
            v = new float[2];
        }
    }

    public static void main(String[] args) {
        StdDraw.enableDoubleBuffering();
        // Set the background color to black
        StdDraw.clear(StdDraw.BLACK);

        int numCircles = 1000;
        Circle[] circles = new Circle[numCircles];

        // Initialize circles with random values
        for (int i = 0; i < circles.length; i++) {
            circles[i] = new Circle();
            
            // Random color
            circles[i].color[0] = (float) Math.random(); // R
            circles[i].color[1] = (float) Math.random(); // G
            circles[i].color[2] = (float) Math.random(); // B

            // Random radius (sane value for screen size 0..1)
            circles[i].r = (float) (0.005 + Math.random() * 0.005);

            // Random position (sane value for screen size 0..1)
            circles[i].p[0] = (float) Math.random();
            circles[i].p[1] = (float) Math.random();

            // Random velocity
            circles[i].v[0] = (float) (0.5 * (Math.random() - 0.5));
            circles[i].v[1] = (float) (0.5 * (Math.random() - 0.5));
        }

        // Initialize time
        long lastTime = System.nanoTime();

        while (true) {
            // Physics update

            long currentTime = System.nanoTime();
            float dt = (float) ((currentTime - lastTime) / 1.0e9); // Time elapsed in seconds
            lastTime = currentTime;

            // Update position
            for (Circle c : circles) {
                c.p[0] += c.v[0] * dt;
                c.p[1] += c.v[1] * dt;
            }

            // Collision detection with screen edges
            for (Circle c : circles) {
                // Left edge
                if (c.p[0] - c.r < 0) {
                    c.v[0] = -c.v[0];
                    c.p[0] = c.r;
                }
                // Right edge
                if (c.p[0] + c.r > 1.0) {
                    c.v[0] = -c.v[0];
                    c.p[0] = 1.0f - c.r;
                }
                // Bottom edge
                if (c.p[1] - c.r < 0) {
                    c.v[1] = -c.v[1];
                    c.p[1] = c.r;
                }
                // Top edge
                if (c.p[1] + c.r > 1.0) {
                    c.v[1] = -c.v[1];
                    c.p[1] = 1.0f - c.r;
                }
            }

            // Circle-circle collisions
            for (int i = 0; i < circles.length; i++) {
                for (int j = i + 1; j < circles.length; j++) {
                    Circle c1 = circles[i];
                    Circle c2 = circles[j];

                    float dx = c2.p[0] - c1.p[0];
                    float dy = c2.p[1] - c1.p[1];
                    float d2 = dx * dx + dy * dy;
                    float dist = (float) Math.sqrt(d2);

                    if (dist < c1.r + c2.r && dist > 0) {
                        float nx = dx / dist;
                        float ny = dy / dist;

                        // Mass proportional to area
                        float m1 = c1.r * c1.r;
                        float m2 = c2.r * c2.r;

                        // Resolve Overlap
                        float overlap = c1.r + c2.r - dist;
                        float moveFactor = overlap / (m1 + m2);
                        c1.p[0] -= moveFactor * m2 * nx;
                        c1.p[1] -= moveFactor * m2 * ny;
                        c2.p[0] += moveFactor * m1 * nx;
                        c2.p[1] += moveFactor * m1 * ny;

                        // Resolve Velocity (Elastic Collision)
                        float dvx = c1.v[0] - c2.v[0];
                        float dvy = c1.v[1] - c2.v[1];
                        float vn = dvx * nx + dvy * ny;

                        if (vn > 0) {
                            float impulse = (2.0f * vn) / (m1 + m2);
                            c1.v[0] -= impulse * m2 * nx;
                            c1.v[1] -= impulse * m2 * ny;
                            c2.v[0] += impulse * m1 * nx;
                            c2.v[1] += impulse * m1 * ny;
                        }
                    }
                }
            }

            // Render circles
            StdDraw.clear(StdDraw.BLACK);
            for (Circle c : circles) {
                // Use the circle's color
                StdDraw.setPenColor(new Color(c.color[0], c.color[1], c.color[2]));
                // Draw the filled circle
                StdDraw.filledCircle(c.p[0], c.p[1], c.r);
            }
            StdDraw.show();
        }
    }
}
