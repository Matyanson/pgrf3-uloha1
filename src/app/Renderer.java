package app;

import app.solid.Axis;
import app.solid.Grid;
import app.solid.Solid;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


public class Renderer extends AbstractRenderer {

    // Solids
    private Solid axis, plane, sphere;

    // Shader programs
    private int programAxis, programGrid;

    // SceneCameras
    private List<SceneWindow> sceneWindowList = new ArrayList<>();

    // Camera Controls
    boolean mouseButton1 = false;
    int selectedCameraIndex = 0;
    double ox, oy;

    // Shadow mapping
    private OGLTexture2D.Viewer viewer;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 2.0f);
        glEnable(GL_DEPTH_TEST);
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        // Default camera
        Camera defCamera = new Camera()
                .withPosition(new Vec3D(0, 0, 0))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-75))
                .withFirstPerson(false)
                .withRadius(3);
        Mat4PerspRH defProj = new Mat4PerspRH(Math.toRadians(45), height / (float) width, 0.1, 100);
        sceneWindowList.add(new SceneWindow(width, height, defCamera, defProj));

        // Shadow mapping camera
        Vec3D lightPosition = new Vec3D(0, 0, 1);
        Camera cameraLight = new Camera()
                .withPosition(lightPosition)
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-90))
                .withFirstPerson(true);
        Mat4 projLight = new Mat4OrthoRH(5, 5, 1, 20);
        sceneWindowList.add(new SceneWindow(width, height, cameraLight, projLight));

        // Solids
        axis = new Axis();
        plane = new Grid(2, 2);
        sphere = new Grid(100, 100);
        sphere.setModel(new Mat4Scale(0.5f, 0.5f, 0.5f).mul(new Mat4Transl(0f, 0, 1f)));

        // Shader programs
        programAxis = ShaderUtils.loadProgram("/axis");
        programGrid = ShaderUtils.loadProgram("/grid");

        viewer = new OGLTexture2D.Viewer();
    }

    public void display() {
        // draw the first SceneWindow to the Screen
        SceneWindow firstCamera = sceneWindowList.get(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        drawScene(firstCamera);

        // draw the rest into textures
        for (int i = 1; i < sceneWindowList.size(); i++) {
            SceneWindow camera = sceneWindowList.get(i);
            camera.getRenderTarget().bind();
            drawScene(camera);
        }

        // Draw the shadowMap in the corner
        SceneWindow secondCamera = sceneWindowList.get(1);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        viewer.view(secondCamera.getRenderTarget().getDepthTexture(), -1, -1, 0.5);
    }


    private void drawScene(SceneWindow camera) {
        // camera.getRenderTarget().bind();
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Draw Axis
        glUseProgram(programAxis);
        setGlobalUniforms(camera, programAxis, axis);
        axis.getBuffers().draw(GL_LINES, programAxis);

        // Draw Plane
        glUseProgram(programGrid);
        setGlobalUniforms(camera, programGrid, plane);
        glUniform1i(glGetUniformLocation(programGrid, "uUseShadowMap"), 1);
        glUniform1i(glGetUniformLocation(programGrid, "uFuncType"), 0);
        glUniform3f(glGetUniformLocation(programGrid, "uBaseColor"), 0.8f, 0.8f, 0.8f);
        plane.getBuffers().draw(GL_TRIANGLES, programGrid);

        // Draw Sphere
        glUseProgram(programGrid);
        setGlobalUniforms(camera, programGrid, sphere);
        glUniform1i(glGetUniformLocation(programGrid, "uUseShadowMap"), 0);
        glUniform1i(glGetUniformLocation(programGrid, "uFuncType"), 1);
        glUniform3f(glGetUniformLocation(programGrid, "uBaseColor"), 0.8f, 0.3f, 0.3f);

        SceneWindow lightCamera = sceneWindowList.get(1);
        lightCamera.getRenderTarget().bindDepthTexture(programGrid, "shadowMap", 0);
        // renderTarget.bindDepthTexture(programGrid, "shadowMap", 0);
        glUniformMatrix4fv(
                glGetUniformLocation(programGrid, "uVPLight"),
                false,
                lightCamera.getView().mul(lightCamera.getProjection()).floatArray()
        );
        sphere.getBuffers().draw(GL_TRIANGLES, programGrid);
    }

    private void setGlobalUniforms(SceneWindow camera, int shaderProgram, Solid solid) {
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, camera.getView().floatArray());

        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, camera.getProjection().floatArray());

        int locUModel = glGetUniformLocation(shaderProgram, "uModel");
        glUniformMatrix4fv(locUModel, false, solid.getModel().floatArray());
    }


    private GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                mouseButton1 = true;
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);
                ox = xBuffer.get(0);
                oy = yBuffer.get(0);
            }

            if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE) {
                mouseButton1 = false;
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);
                double x = xBuffer.get(0);
                double y = yBuffer.get(0);
                SceneWindow selectedSceneWindow = sceneWindowList.get(selectedCameraIndex);
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .addAzimuth((double) Math.PI * (ox - x) / width)
                                .addZenith((double) Math.PI * (oy - y) / width)
                );
                ox = x;
                oy = y;
            }
        }

    };

    private GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mouseButton1) {
                SceneWindow selectedSceneWindow = sceneWindowList.get(selectedCameraIndex);
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .addAzimuth((double) Math.PI * (ox - x) / width)
                                .addZenith((double) Math.PI * (oy - y) / width)
                );
                ox = x;
                oy = y;
            }
        }
    };


    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            SceneWindow selectedSceneWindow = sceneWindowList.get(selectedCameraIndex);
            if (dy < 0){
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .mulRadius(0.9f)
                );
            }
            else{
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .mulRadius(1.1f)
                );
            }
        }
    };


    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mbCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cpCallbacknew;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }


}