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

                try {

                    String reason = "VAMSHI CRASH: "
                            + error.getClass().getSimpleName()
                            + " - " + error.getMessage();

                    if (error.getCause() != null) {
                        reason = reason + " CAUSED BY: "
                                + error.getCause().getClass().getSimpleName()
                                + " - " + error.getCause().getMessage();
                    }

                    final String finalReason = reason;

                    Handler mainHandler = new Handler(Looper.getMainLooper());

                    mainHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(
                                    getApplicationContext(),
                                    finalReason,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                    Thread.sleep(2500);

                } catch (Exception ignored) {
                }

                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, error);
                }
            }
        });
    }
}
