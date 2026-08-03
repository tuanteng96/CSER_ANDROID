package vn.cser21;

import android.os.Bundle;

import androidx.core.view.WindowCompat;

import com.journeyapps.barcodescanner.CaptureActivity;

public class EdgeToEdgeCaptureActivity extends CaptureActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
    }
}
