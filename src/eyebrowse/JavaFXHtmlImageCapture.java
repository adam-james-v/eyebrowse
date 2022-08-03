// Modified from: https://gist.github.com/danialfarid/2ddbab04803ae4fd2dca
// credit to GitHub User danialfarid (Danial Farid) for that gist

package eyebrowse;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class JavaFXHtmlImageCapture extends Application {
    private static WebView webView;
    private static Stage stage;

    @PostConstruct
    public Thread initialize() {
        System.setProperty("javafx.macosx.embedded", "true");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        initMonocleHeadless(true);
        Thread theThread = new Thread(() -> {
            Application.launch(JavaFXHtmlImageCapture.class);
        });
        return theThread;
    }

    @Override
    public void start(Stage stage) throws Exception {
        JavaFXHtmlImageCapture.stage = stage;
        webView = new WebView();
        webView.setPrefSize(800, 100);

        webView.getEngine().load("about:blank");

        VBox layout = new VBox(10);
        layout.getChildren().setAll(webView);

        stage.setScene(new Scene(layout));
        stage.show();
    }

    public synchronized static BufferedImage captureHtml(String html, double W, double H) {
        AtomicReference<BufferedImage> captured = new AtomicReference<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        runLater(() -> {
            try {
                double width = W + 15;
                double height = H + 15;
                webView.setPrefSize(width, height);
                webView.setMaxWidth(width);
                if (html.startsWith("http")) {
                    webView.getEngine().load(html);
                } else {
                    webView.getEngine().loadContent(html, "text/html");
                }
                webView.autosize();
                stage.show();

                runLater(() -> {
                    try {
                        String heightText = webView.getEngine().executeScript("document.body.offsetHeight").toString();
                        double documentHeight = Double.valueOf(heightText) + 15;
                        stage.setHeight(height);
                        webView.setPrefHeight(height);
                        runLater(() -> {
                            try {
                                SnapshotParameters parameters = new SnapshotParameters();
                                parameters.setViewport(new Rectangle2D(0, 0, width - 15, Math.max(height, documentHeight) - 15));

                                WritableImage image = webView.snapshot(parameters, null);
                                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                                captured.set(bufferedImage);
//                                ImageIO.write(bufferedImage, "png", new File("capture.png"));
                            } catch (Throwable t) {
                                throwable.set(t);
                            }
                        });
                    } catch (Throwable t) {
                        throwable.set(t);
                    }
                });
            } catch (Throwable t) {
                throwable.set(t);
            }
        });
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 100; i++) {
            if (captured.get() == null && throwable.get() == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (throwable.get() != null) {
            throw new RuntimeException(throwable.get());
        }
        return captured.get();
    }

    public static void runInPlatform(Runnable runnable) {
        Platform.runLater(() -> {
            final AnimationTimer timer = new AnimationTimer() {
                private int pulseCounter;

                @Override
                public void handle(long now) {
                    pulseCounter += 1;
                    if (pulseCounter > 2) {
                        stop();
                        runnable.run();
                    }
                }
            };
            timer.start();
        });
    }

    public static void runLater(Runnable runnable) {
        final PauseTransition pt = new PauseTransition();
        pt.setDuration(Duration.millis(100));
        pt.setOnFinished(actionEvent -> runInPlatform(runnable::run));
        pt.play();
    }

    private static void initMonocleHeadless(boolean headless) {
        if (checkSystemPropertyEquals("testfx.headless", "true") || headless) {
            try {
                assignMonoclePlatform();
                assignHeadlessPlatform();
            } catch (ClassNotFoundException var3) {
                throw new IllegalStateException("Monocle headless platform not found", var3);
            } catch (Exception var4) {
                throw new RuntimeException(var4);
            }
        }

    }

    private static boolean checkSystemPropertyEquals(String propertyName, String valueOrNull) {
        return Objects.equals(System.getProperty(propertyName, null), valueOrNull);
    }

    private static void assignMonoclePlatform() throws Exception {
        Class platformFactoryClass = Class.forName("com.sun.glass.ui.PlatformFactory");
        Object platformFactoryImpl = Class.forName("com.sun.glass.ui.monocle.MonoclePlatformFactory").newInstance();
        assignPrivateStaticField(platformFactoryClass, "instance", platformFactoryImpl);
    }

    // https://gist.github.com/danialfarid/2ddbab04803ae4fd2dca?permalink_comment_id=3088577#gistcomment-3088577
    // credit to github user hmf for this code change
    private static void assignHeadlessPlatform() throws Exception {
        Class<?> nativePlatformFactoryClass = Class.forName("com.sun.glass.ui.monocle.NativePlatformFactory");
        try {
            Constructor<?> nativePlatformCtor = Class.forName(
                    "com.sun.glass.ui.monocle.HeadlessPlatform").getDeclaredConstructor();
            nativePlatformCtor.setAccessible(true);
            assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
        }
        catch (ClassNotFoundException exception) {
            // Before Java 8u40 HeadlessPlatform was located inside of a "headless" package.
            Constructor<?> nativePlatformCtor = Class.forName(
                    "com.sun.glass.ui.monocle.headless.HeadlessPlatform").getDeclaredConstructor();
            nativePlatformCtor.setAccessible(true);
            assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
        }
    }
    private static void assignPrivateStaticField(Class<?> cls, String name, Object value) throws Exception {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        field.set(cls, value);
        field.setAccessible(false);
    }
}
