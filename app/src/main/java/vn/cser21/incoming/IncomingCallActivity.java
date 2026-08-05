package vn.cser21.incoming;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import vn.cser21.MainActivity;
import vn.cser21.R;

public class IncomingCallActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer mediaPlayer;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EndCallEvent event) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.release();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureEdgeToEdgeWindow();
        setContentView(R.layout.activity_incoming_call);

        View root = findViewById(R.id.incoming_call_root);
        View actions = findViewById(R.id.clAction);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            actions.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        if (WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()) != null) {
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightNavigationBars(false);
        }

        if (getIntent().getExtras().getBoolean("IS_MAIN", false) == true) {
            mediaPlayer = MediaPlayer.create(this, R.raw.call);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }

        String url = getIntent().getExtras().getString("URL_MP3");
        int id = getIntent().getExtras().getInt("ID", 0);

        findViewById(R.id.llAccept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent acceptCallIntent = new Intent(IncomingCallActivity.this, CallingMeetingActivity.class);
                acceptCallIntent.putExtra("URL_MP3", url);
                acceptCallIntent.putExtra("ID", id);
                startActivity(acceptCallIntent);
                finish();
            }
        });

        findViewById(R.id.llReject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(IncomingCallActivity.this, IncomingCallNotificationService.class));
                if (getIntent().getExtras().getBoolean("IS_MAIN", false) != true) {
                    Intent intent = new Intent(IncomingCallActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                finish();
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopService(new Intent(IncomingCallActivity.this, IncomingCallNotificationService.class));
                    Intent intent = new Intent(IncomingCallActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                finish();
            }
        }, 60000L);

    }

    private void configureEdgeToEdgeWindow() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }

}
