package app.solid;

import lwjglutils.OGLBuffers;

import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;

public class GridStrip extends Solid{

    public GridStrip(int m, int n) {
        float[] vb = new float[2 * m * n];
        int[] ib = new int[3 * 2 * (m - 1) * (n - 1)];

        // Vertexes
        int index = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                vb[index++] = j / (float) (n - 1);
                vb[index++] = i / (float) (m - 1);
            }
        }

        // Indexes
        index = 0;
        for (int i = 0; i < m - 1; i++) {
            int offset = i * n;
            for (int j = 0; j < n; j++) {
                ib[index++] = offset + j;          // Current row
                ib[index++] = offset + n + j;      // Next row
            }

            // Separate rows
            ib[index++] = (i + 1) * n + (n - 1); // Last vertex of current row
            ib[index++] = (i + 1) * n;          // First vertex of next row
        }

        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2),
        };

        buffers = new OGLBuffers(vb, attributes, ib);
        topology = GL_TRIANGLE_STRIP;
    }
}
