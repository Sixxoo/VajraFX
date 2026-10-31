package cz.sixxo.vajrafx;

import java.nio.FloatBuffer;

/** Small matrix and vector helpers (OpenGL column-major matrices, eye space looks down -Z). */
final class FxMath {

    private FxMath() {}

    /** Multiplies an affine point by a 4x4 matrix. */
    static float[] transform(FloatBuffer m, float x, float y, float z) {
        return new float[] { m.get(0) * x + m.get(4) * y + m.get(8) * z + m.get(12),
            m.get(1) * x + m.get(5) * y + m.get(9) * z + m.get(13),
            m.get(2) * x + m.get(6) * y + m.get(10) * z + m.get(14) };
    }

    /** Projects an eye-space point to normalised device coordinates (-1..1), or null when behind the camera. */
    static float[] project(FloatBuffer p, float[] e) {
        float cx = p.get(0) * e[0] + p.get(4) * e[1] + p.get(8) * e[2] + p.get(12);
        float cy = p.get(1) * e[0] + p.get(5) * e[1] + p.get(9) * e[2] + p.get(13);
        float cw = p.get(3) * e[0] + p.get(7) * e[1] + p.get(11) * e[2] + p.get(15);
        if (cw <= 1e-4F) return null;
        return new float[] { cx / cw, cy / cw };
    }

    /** Eye-space point at the given distance in front of the camera that lands on screen position (nx, ny). */
    static float[] unproject(FloatBuffer p, float nx, float ny, float depth) {
        float z = -depth;
        float x = (nx * depth - p.get(8) * z - p.get(12)) / p.get(0);
        float y = (ny * depth - p.get(9) * z - p.get(13)) / p.get(5);
        return new float[] { x, y, z };
    }

    /**
     * Eye-space point back to camera-relative world coordinates. The camera matrix only rotates and
     * translates (view bobbing included), so its inverse is the transposed rotation.
     */
    static float[] eyeToRelative(FloatBuffer m, float[] e) {
        float a = e[0] - m.get(12), b = e[1] - m.get(13), c = e[2] - m.get(14);
        return new float[] { m.get(0) * a + m.get(1) * b + m.get(2) * c, m.get(4) * a + m.get(5) * b + m.get(6) * c,
            m.get(8) * a + m.get(9) * b + m.get(10) * c };
    }

    static float[] sub(float[] a, float[] b) {
        return new float[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
    }

    static float[] cross(float[] a, float[] b) {
        return new float[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    static float len(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    static float[] norm(float[] v) {
        float l = len(v);
        if (l < 1e-6F) return new float[] { 0, 1, 0 };
        return new float[] { v[0] / l, v[1] / l, v[2] / l };
    }

    static float[] lerp(float[] a, float[] b, float t) {
        return new float[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t };
    }
}
