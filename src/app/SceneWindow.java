package app;

import lwjglutils.OGLRenderTarget;
import transforms.*;

// This class is representing a view into a scene
public class SceneWindow {

    private Camera camera;
    private Mat4 projection;
    private OGLRenderTarget renderTarget;

    public SceneWindow(int width, int height, Camera camera, Mat4 projection) {
        this.renderTarget = new OGLRenderTarget(width, height);
        this.camera = camera;
        this.projection = projection;
    }

    public SceneWindow(int width, int height) {
        this.renderTarget = new OGLRenderTarget(width, height);
        this.camera = new Camera()
                .withPosition(new Vec3D(0, 0, 0))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-75))
                .withFirstPerson(false)
                .withRadius(3);
        this.projection = new Mat4PerspRH(Math.toRadians(45), height / (float) width, 0.1, 100);
    }

    public OGLRenderTarget getRenderTarget() {
        return renderTarget;
    }

    public void setRenderTarget(OGLRenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Mat4 getProjection() {
        return projection;
    }

    public void setProjection(Mat4PerspRH projection) {
        this.projection = projection;
    }

    public Mat4 getView() {
        return this.camera.getViewMatrix();
    }
}
