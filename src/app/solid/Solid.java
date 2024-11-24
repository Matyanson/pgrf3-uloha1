package app.solid;

import lwjglutils.OGLBuffers;
import transforms.Mat4;
import transforms.Mat4Identity;

public abstract class Solid {
    protected OGLBuffers buffers;
    protected int topology;
    private Mat4 model = new Mat4Identity();

    public OGLBuffers getBuffers() {
        return buffers;
    }

    public void draw(int program) {
        buffers.draw(topology, program);
    }

    public Mat4 getModel() {
        return model;
    }

    public void setModel(Mat4 model) {
        this.model = model;
    }

    public int getTopology() {
        return topology;
    }

    public void setTopology(int topology) {
        this.topology = topology;
    }
}
