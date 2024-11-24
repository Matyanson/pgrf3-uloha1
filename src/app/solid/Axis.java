package app.solid;

import lwjglutils.OGLBuffers;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class Axis extends Solid{
    public Axis() {
        // vytvořit vertex buffer
        float[] vb = {
            // x
            0, 0, 0,   1, 0, 0,
            1, 0, 0,   1, 0, 0,
            // y
            0, 0, 0,   0, 1, 0,
            0, 1, 0,   0, 1, 0,
            // z
            0, 0, 0,   0, 0, 1,
            0, 0, 1,   0, 0, 1,
        };

        // vytvořit index buffer
        int[] ib = {
            0, 1,
            2, 3,
            4, 5
        };

        // vytvořit OGLBuffers.Attrib[] attributes
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
        };

        // vytvořit OGLBuffers
        buffers = new OGLBuffers(vb, attributes, ib);
        topology = GL_LINES;
    }
}
