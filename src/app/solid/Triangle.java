package app.solid;

import lwjglutils.OGLBuffers;

public class Triangle extends Solid {

    public Triangle() {
        // vytvořit vertex buffer
        float[] vb = {
                0,  1, 0,    1, 0, 0,
                -1,  0, 0,    0, 1, 0,
                1, -1, 0,    0, 0, 1,
        };

        // vytvořit index buffer
        int[] ib = {
                0, 1, 2
        };

        // vytvořit OGLBuffers.Attrib[] attributes
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
        };

        // vytvořit OGLBuffers
        buffers = new OGLBuffers(vb, attributes, ib);

    }
}
