package app.solid;

import lwjglutils.OGLBuffers;

import static org.lwjgl.opengl.GL11.*;

public class Flame extends Solid{
    public Flame(int n) {
        // vytvořit vertex buffer
        float[] vb = new float[n*2];
        for (int i = 0; i < n; i++) {
            vb[i*2] = (float)i/n;
            vb[i*2 + 1] = (float)Math.random();
        }

        // vytvořit index buffer
        int[] ib = new int[n];
        for (int i = 0; i < n; i++) {
            ib[i] = i;
        }

        // vytvořit OGLBuffers.Attrib[] attributes
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inNormalizedIndex", 1),
                new OGLBuffers.Attrib("inTimeOffset", 1),
        };

        // vytvořit OGLBuffers
        buffers = new OGLBuffers(vb, attributes, ib);
        topology = GL_POINTS;
    }
}
