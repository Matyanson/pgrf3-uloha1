package app;

import app.solid.Axis;
import app.solid.Grid;
import app.solid.GridStrip;
import app.solid.Solid;
import app.uniform_values.UniformFValues;
import app.uniform_values.UniformInt1Values;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


public class Renderer extends AbstractRenderer {

    // Solids
    private List<Solid> solids;

    // Shader programs
    private List<ShaderProgram> shaderPrograms;

    // SceneCameras
    private List<SceneWindow> sceneWindowList = new ArrayList<>();

    // Shadow mapping
    private OGLTexture2D.Viewer viewer;

    // Camera Controls
    boolean mouseButton1 = false;
    int selectedCameraIndex = 1;
    double ox, oy;

    // Other Controls
    int polygonMode = GL_FILL;


    public void init() {
        // Behaviour
        glClearColor(0.1f, 0.1f, 0.1f, 2.0f);
        glEnable(GL_DEPTH_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

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
        Axis axis = new Axis();
        Grid plane = new Grid(20, 20);
        GridStrip planeStrip = new GridStrip(20, 20);
        planeStrip.setModel(new Mat4Transl(2f, 0f, 0f));
        Grid sphere = new Grid(100, 100);
        sphere.setModel(new Mat4Scale(0.5f, 0.5f, 0.5f).mul(new Mat4Transl(0f, 0, 1f)));
        solids = Arrays.asList(axis, plane, planeStrip, sphere);

        // Shader programs
        ShaderProgram programAxis = new ShaderProgram("/axis", 0);
        ShaderProgram programGrid = new ShaderProgram("/grid", 1, 2, 3);
        programGrid.addUniform(new UniformInt1Values("uUseShadowMap",
                1, 1, 1, 0
        ));
        programGrid.addUniform(new UniformInt1Values("uFuncType",
                0, 0, 0, 1
        ));
        programGrid.addUniform(new UniformFValues("uBaseColor",
                new Float[]{0.8f, 0.8f, 0.8f},
                new Float[]{0.8f, 0.8f, 0.8f},
                new Float[]{0.3f, 0.8f, 0.3f},
                new Float[]{0.8f, 0.3f, 0.3f}
        ));
        shaderPrograms = Arrays.asList(programAxis, programGrid);

        // Viewer to display other textures
        viewer = new OGLTexture2D.Viewer();
    }

    public void display() {
        // draw the rest into textures
        for (int i = 0; i < sceneWindowList.size(); i++) {
            drawScene(i);
        }

        // Draw the shadowMap in the corner
        SceneWindow lightWindow = sceneWindowList.get(1);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        viewer.view(lightWindow.getRenderTarget().getDepthTexture(), -1, -1, 0.5);
    }


    private void drawScene(int sceneWindowIndex) {
        // Bind buffer / renderTarget
        SceneWindow sceneWindow = sceneWindowList.get(sceneWindowIndex);
        if(sceneWindowIndex == 0)
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        else
            sceneWindow.getRenderTarget().bind();

        // Setup canvas
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


        for (int i = 0; i < shaderPrograms.size(); i++) {
            ShaderProgram program = shaderPrograms.get(i);
            glUseProgram(program.getProgramID());
            setCameraUniforms(sceneWindow, program.getProgramID());

            List<Integer> solidIndexes = program.getSolidIndexes();
            for (int j : solidIndexes) {
                Solid solid = solids.get(j);
                // setup uniforms
                setModelUniform(program.getProgramID(), solid);
                program.applyUniforms(j);
                // Specific/Complex behaviour
                manualSetupBeforeDraw(sceneWindowIndex, i, j, sceneWindow, program, solid);
                // draw
                solid.draw(program.getProgramID());
            }
        }
    }

    private void manualSetupBeforeDraw(
            int windowIndex,            int programIndex,       int solidIndex,
            SceneWindow sceneWindow,    ShaderProgram program,  Solid solid
    ) {
        if(windowIndex == 0) {
            // save shadowMap to texture
            SceneWindow lightWindow = sceneWindowList.get(1);
            lightWindow.getRenderTarget()
                    .bindDepthTexture(program.getProgramID(), "shadowMap", 0);
            // save light VP matrix to uniform
            glUniformMatrix4fv(
                    glGetUniformLocation(program.getProgramID(), "uVPLight"),
                    false,
                    lightWindow.getView().mul(lightWindow.getProjection()).floatArray()
            );
        }
    }

    private void setCameraUniforms(SceneWindow sceneWindow, int shaderProgram) {
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, sceneWindow.getView().floatArray());

        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, sceneWindow.getProjection().floatArray());
    }
    private void setModelUniform(int shaderProgram, Solid solid) {
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

    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            switch (action) {
                case GLFW_PRESS:
                    switch (key) {
                        case GLFW_KEY_M:
                            // Change PolygonMode
                            Integer[] options = {GL_FILL, GL_LINE, GL_POINT};
                            int index = Arrays.asList(options).indexOf(polygonMode);
                            if (index < 0 || index == options.length - 1) {
                                polygonMode = options[0];
                            } else {
                                polygonMode = options[index + 1];
                            }
                            glPolygonMode(GL_FRONT_AND_BACK, polygonMode);
                            break;
                    }
                    break;
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

    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

}