import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class VamshiApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable error) {

                StringBuilder sb = new StringBuilder();

                sb.append("CRASH: ")
                  .append(error.getClass().getSimpleName())
                  .append("\n")
                  .append(error.getMessage())
                  .append("\n");

                StackTraceElement[] stack = error.getStackTrace();

                for (StackTraceElement element : stack) {

                    String line = element.toString();

                    if (line.contains("vamshi")
                            || line.contains("Vamshi")
                            || line.contains("MainActivity")) {

                        sb.append(line).append("\n");
                    }
                }

                if (error.getCause() != null) {

                    sb.append("CAUSED BY: ")
                      .append(error.getCause().toString());
                }

                final String reason = sb.toString();

                new Handler(Looper.getMainLooper())
                        .post(new Runnable() {

                    @Override
                    public void run() {

                        Toast.makeText(
                                getApplicationContext(),
                                reason,
                                Toast.LENGTH_LONG
                        ).show();

                        Looper.loop();
                    }
                });
            }
        });
    }
}
