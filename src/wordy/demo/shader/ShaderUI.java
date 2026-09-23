package wordy.demo.shader;

import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JFrame;

import wordy.ast.StatementNode;
import wordy.demo.ExecutionCancelledException;
import wordy.parser.WordyParser;

/**
 * An interactive Java UI that uses a Wordy-based shader to generate an image, which the user can
 * click on to zoom.
 */
public class ShaderUI {
    private static final boolean USE_COMPILER = true; // Compiler is faster
    // false Done rendering (378ms)
    // true Done rendering (95ms)
    private final StatementNode program;
    private final BufferedImage image;
    private final JFrame window;
    private double centerX, centerY, scale;

    private Renderer currentRenderer;
    private final Executor renderWorker = Executors.newFixedThreadPool(1);

    public static void main(String[] args) throws Exception {
        new ShaderUI(
            "ripples.wordy",  // Simple test involving only assignments and expressions
//            "mandel.wordy",   // More complex test involving loops and conditionals
            600, 600
        );
    }

    public ShaderUI(String sourceFileName, int width, int height) throws Exception {
        program = loadProgram("/" + sourceFileName);

        final int pixelRatio = 2;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        window = new ImageWindow(sourceFileName, image, pixelRatio);

        centerX = 0;
        centerY = 0;
        scale = 2.0 / width;

        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double x = (e.getX() - window.getInsets().left) * pixelRatio - width  / 2;
                double y = (e.getY() - window.getInsets().top)  * pixelRatio - height / 2;
                double newScale = scale * (e.isAltDown() ? 3.0 : 1/3.0);
                centerX += x * (scale - newScale);
                centerY += y * (scale - newScale);
                scale = newScale;
                render();
            }
        });

        render();
    }

    private static StatementNode loadProgram(String sourceFile) throws IOException, URISyntaxException {
        String source = new String(Files.readAllBytes(Paths.get(
            ShaderUI.class.getResource(sourceFile).toURI())));
        System.out.println(source);
        System.out.println();

        var program = WordyParser.parseProgram(source);
        System.out.println(program.dump());
        System.out.println();

        return program;
    }

    private void render() {
        Renderer renderer = new Renderer(
            image, centerX, centerY, scale,
            USE_COMPILER
                ? new CompiledShader(program, scale)
                : new InterpretedShader(program, scale));

        synchronized(this) {
            currentRenderer = renderer;
        }

        renderer.onProgress(() -> {
            synchronized(this) {
                if(renderer != currentRenderer) {
                    throw new ExecutionCancelledException();
                }
            }
            window.repaint(100);
        });

        renderWorker.execute(renderer);
    }

    private static class ImageWindow extends JFrame {
        private final BufferedImage image;
        private final int pixelRatio;

        public ImageWindow(String title, BufferedImage image, int pixelRatio) throws HeadlessException {
            super(title);
            this.image = image;
            this.pixelRatio = pixelRatio;
            pack();
            setSize(
                image.getWidth()  / pixelRatio + getInsets().left + getInsets().right,
                image.getHeight() / pixelRatio + getInsets().top + getInsets().bottom);
            setResizable(false);
            setVisible(true);
        }

        @Override
        public void paint(Graphics g) {
            g.drawImage(image, getInsets().left, getInsets().top, image.getWidth() / pixelRatio, image.getHeight() / pixelRatio, null);
        }
    }
}
