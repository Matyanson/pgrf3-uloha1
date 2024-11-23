package app;

import app.solid.Axis;
import app.solid.Grid;
import app.solid.Solid;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import transforms.*;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


public class Renderer extends AbstractRenderer {

    // Solids
    private Solid axis, plane, sphere;

    // Shaders
    private int programAxis, programGrid;

    // Camera
    private Camera camera;
    private Mat4PerspRH proj;
    boolean mouseButton1 = false;
    double ox, oy;

    // Shadow mapping
    private Mat4OrthoRH projLight;
    private Mat4 viewLight;
    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer viewer;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 2.0f);
        glEnable(GL_DEPTH_TEST);
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        camera = new Camera()
                .withPosition(new Vec3D(0, 0, 0))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-75))
                .withFirstPerson(false)
                .withRadius(3);

        proj = new Mat4PerspRH(Math.toRadians(45), height / (float) width, 0.1, 100);

        axis = new Axis();
        plane = new Grid(2, 2);
        sphere = new Grid(100, 100);
        sphere.setModel(new Mat4Scale(0.5f, 0.5f, 0.5f).mul(new Mat4Transl(0f, 0, 0.7f)));

        // načíst shaderový program
        programAxis = ShaderUtils.loadProgram("/axis");
        programGrid = ShaderUtils.loadProgram("/grid");

        // Shadow mapping
        renderTarget = new OGLRenderTarget(width, height);
        projLight = new Mat4OrthoRH(5, 5, 1, 20);
        Vec3D lightPosition = new Vec3D(0, 0, 1);
        viewLight = new Mat4ViewRH(lightPosition, lightPosition.mul(-1), new Vec3D(0, 1, 0));

        viewer = new OGLTexture2D.Viewer();
    }

    public void display() {
        renderTarget.bind();
        drawScene(true);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        drawScene(false);

        viewer.view(renderTarget.getDepthTexture(), -1, -1, 0.5);
    }


    private void drawScene(boolean drawFromLight) {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Draw axis
        if(!drawFromLight) {
            glUseProgram(programAxis);
            setGlobalUniforms(axis, programAxis, drawFromLight);
            axis.getBuffers().draw(GL_LINES, programAxis);
        }

        // Draw grid
        glUseProgram(programGrid);
        setGlobalUniforms(plane, programGrid, drawFromLight);
        glUniform1i(glGetUniformLocation(programGrid, "uUseShadowMap"), 1);
        glUniform1i(glGetUniformLocation(programGrid, "uFuncType"), 0);
        glUniform3f(glGetUniformLocation(programGrid, "uBaseColor"), 0.8f, 0.8f, 0.8f);
        plane.getBuffers().draw(GL_TRIANGLES, programGrid);

        // Draw grid
        glUseProgram(programGrid);
        setGlobalUniforms(sphere, programGrid, drawFromLight);
        glUniform1i(glGetUniformLocation(programGrid, "uUseShadowMap"), 0);
        glUniform1i(glGetUniformLocation(programGrid, "uFuncType"), 1);
        glUniform3f(glGetUniformLocation(programGrid, "uBaseColor"), 0.8f, 0.3f, 0.3f);
        if(!drawFromLight) {
            renderTarget.getDepthTexture().bind(programGrid, "shadowMap", 0);
            glUniformMatrix4fv(glGetUniformLocation(programGrid, "uVPLight"), false, (viewLight.mul(projLight)).floatArray());
        }
        sphere.getBuffers().draw(GL_TRIANGLES, programGrid);
    }

    private void setGlobalUniforms(Solid solid, int shaderProgram, boolean drawFromLight) {
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        int locUModel = glGetUniformLocation(shaderProgram, "uModel");

        if (drawFromLight)
            glUniformMatrix4fv(locUView, false, viewLight.floatArray());
        else glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());

        if (drawFromLight)
            glUniformMatrix4fv(locUProj, false, projLight.floatArray());
        else
            glUniformMatrix4fv(locUProj, false, proj.floatArray());

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
                camera = camera.addAzimuth((double) Math.PI * (ox - x) / width)
                        .addZenith((double) Math.PI * (oy - y) / width);
                ox = x;
                oy = y;
            }
        }

    };

    private GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mouseButton1) {
                camera = camera.addAzimuth((double) Math.PI * (ox - x) / width)
                        .addZenith((double) Math.PI * (oy - y) / width);
                ox = x;
                oy = y;
            }
        }
    };


    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            if (dy < 0)
                camera = camera.mulRadius(0.9f);
            else
                camera = camera.mulRadius(1.1f);

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