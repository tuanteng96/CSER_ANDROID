package vn.cser21;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import pub.devrel.easypermissions.EasyPermissions;

public class QRCodeFragment extends Fragment {

    private static final int MAX_BITMAP_DIMENSION = 1024;

    public static QRCodeFragment newInstance(QRCodeResult listener) {
        QRCodeFragment fragment = new QRCodeFragment();
        fragment.listener = listener;
        return fragment;
    }

    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private View root;
    private SurfaceView surfaceView;
    private Camera camera;
    private boolean flashmode = false;
    private QRCodeResult listener;
    private boolean overlayAdded = false;
    private View scanOverlay;
    private View scanAreaView;
    private View scanLine;
    private TextView instructionLabel;
    private Button photoButton;
    private FrameLayout qrContainer;
    private boolean isSendNotif = false;
    private boolean isPickingImage = false;
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickedImage);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.activity_qrcode, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View closeButton = root.findViewById(R.id.ivClose);
        View flashButton = root.findViewById(R.id.ivFlash);

        applyStatusBarSpacing(closeButton, flashButton);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeScanner();
            }
        });
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashOnButton();
            }
        });
        qrContainer = root.findViewById(R.id.qrContainer);
        surfaceView = root.findViewById(R.id.sfvCamera);
        methodRequiresPermission();
    }

    private void applyStatusBarSpacing(@NonNull View closeButton, @NonNull View flashButton) {
        final int baseTopMarginPx = dpToPx(20);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            updateTopMargin(closeButton, baseTopMarginPx + topInset);
            updateTopMargin(flashButton, baseTopMarginPx + topInset);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void updateTopMargin(@NonNull View view, int topMargin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        if (marginLayoutParams.topMargin == topMargin) {
            return;
        }

        marginLayoutParams.topMargin = topMargin;
        view.setLayoutParams(marginLayoutParams);
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void methodRequiresPermission() {
        String[] perms = {Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(requireActivity(), perms)) {
            initBarcodeScanner();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (EasyPermissions.hasPermissions(requireActivity(), Manifest.permission.CAMERA)) {
            if (barcodeDetector == null || cameraSource == null) {
                initBarcodeScanner();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseScanner();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseScanner();
        root = null;
        surfaceView = null;
        qrContainer = null;
    }

    private void flashOnButton() {
        if (cameraSource == null) {
            return;
        }
        camera = getCamera(cameraSource);
        if (camera == null) {
            return;
        }
        try {
            Camera.Parameters param = camera.getParameters();
            param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(param);
            flashmode = !flashmode;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    return (Camera) field.get(cameraSource);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    private void initBarcodeScanner() {
        if (!isAdded() || surfaceView == null || barcodeDetector != null || cameraSource != null) {
            return;
        }

        barcodeDetector = new BarcodeDetector.Builder(requireContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(@NonNull Detector.Detections<Barcode> detections) {
                if (!isAdded() || !barcodeDetector.isOperational()) {
                    return;
                }
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes == null || barcodes.size() == 0) {
                    return;
                }
                Barcode barcode = barcodes.valueAt(0);
                if (barcode == null || barcode.displayValue == null || barcode.displayValue.isEmpty()) {
                    return;
                }
                deliverQrCode(barcode.displayValue);
            }
        });

        cameraSource = new CameraSource.Builder(requireContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedFps(35.5f)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @SuppressLint("MissingPermission")
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (cameraSource == null) {
                    return;
                }
                try {
                    cameraSource.start(holder);
                    if (!overlayAdded) {
                        setupScanOverlay();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (cameraSource != null) {
                    cameraSource.stop();
                }
            }
        });
    }

    private void handlePickedImage(@Nullable Uri uri) {
        isPickingImage = false;
        setLifecycleJsSuppressed(false);
        if (uri == null) {
            return;
        }
        String qrCode = detectQrCodeFromImage(uri);
        if (qrCode != null && !qrCode.isEmpty()) {
            deliverQrCode(qrCode);
            return;
        }
        showQrNotFoundDialog();
    }

    @Nullable
    private String detectQrCodeFromImage(@NonNull Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = decodeBitmap(uri);
            if (bitmap == null) {
                return null;
            }

            BarcodeDetector detector = barcodeDetector;
            boolean shouldReleaseDetector = false;
            if (detector == null || !detector.isOperational()) {
                detector = new BarcodeDetector.Builder(requireContext())
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();
                shouldReleaseDetector = true;
            }

            if (!detector.isOperational()) {
                if (shouldReleaseDetector) {
                    detector.release();
                }
                return null;
            }

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(frame);
            if (shouldReleaseDetector) {
                detector.release();
            }

            if (barcodes == null || barcodes.size() == 0) {
                return null;
            }

            Barcode barcode = barcodes.valueAt(0);
            if (barcode == null || barcode.displayValue == null || barcode.displayValue.isEmpty()) {
                return null;
            }
            return barcode.displayValue;
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            return null;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    @Nullable
    private Bitmap decodeBitmap(@NonNull Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }
        try {
            BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            inputStream.close();
        }

        int inSampleSize = 1;
        while (options.outWidth / inSampleSize > MAX_BITMAP_DIMENSION
                || options.outHeight / inSampleSize > MAX_BITMAP_DIMENSION) {
            inSampleSize *= 2;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        inputStream = requireActivity().getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }
        try {
            return BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            inputStream.close();
        }
    }

    private void deliverQrCode(@NonNull String code) {
        if (isSendNotif || listener == null || !isAdded()) {
            return;
        }
        isSendNotif = true;
        listener.onQRCode(code);
        closeScanner();
    }

    private void closeScanner() {
        isPickingImage = false;
        setLifecycleJsSuppressed(false);
        if (!isAdded()) {
            return;
        }
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
    }

    private void showQrNotFoundDialog() {
        if (!isAdded()) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Không tìm thấy mã QR")
                .setMessage("Ảnh không chứa mã QR.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void releaseScanner() {
        if (!isPickingImage) {
            setLifecycleJsSuppressed(false);
        }
        flashmode = false;
        camera = null;
        if (cameraSource != null) {
            cameraSource.stop();
            cameraSource.release();
            cameraSource = null;
        }
        if (barcodeDetector != null) {
            barcodeDetector.release();
            barcodeDetector = null;
        }
    }

    private void setupScanOverlay() {
        if (!isAdded() || qrContainer == null || overlayAdded) {
            return;
        }
        overlayAdded = true;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int scanSide = (int) (screenWidth * 0.68f);
        int topMargin = (screenHeight - scanSide) / 2;

        View overlay = new View(requireContext());
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setBackgroundColor(0x66000000);
        qrContainer.addView(overlay);
        scanOverlay = overlay;

        View scanArea = new View(requireContext());
        FrameLayout.LayoutParams areaParams = new FrameLayout.LayoutParams(scanSide, scanSide);
        areaParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        areaParams.topMargin = topMargin;
        scanArea.setLayoutParams(areaParams);
        scanArea.setBackgroundResource(android.R.color.transparent);
        qrContainer.addView(scanArea);
        scanAreaView = scanArea;

        View line = new View(requireContext());
        FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(scanSide, 4);
        lineParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lineParams.topMargin = topMargin;
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(0xFFFFFFFF);
        qrContainer.addView(line);
        scanLine = line;

        TextView instruction = new TextView(requireContext());
        instruction.setText("Di chuyển camera đến mã QR để quét");
        instruction.setTextColor(0xFFFFFFFF);
        instruction.setTextSize(15);
        instruction.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams instructionParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        instructionParams.gravity = Gravity.TOP;
        instructionParams.leftMargin = 24;
        instructionParams.rightMargin = 24;
        instructionParams.topMargin = topMargin + scanSide + 40;
        instruction.setLayoutParams(instructionParams);
        qrContainer.addView(instruction);
        instructionLabel = instruction;

        Button button = new Button(requireContext());
        button.setText("Chọn từ Thư viện ảnh");
        button.setBackgroundColor(0x00000000);
        button.setTextColor(0xFFFFFFFF);
        button.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);
        button.setCompoundDrawableTintList(ColorStateList.valueOf(0xFFFFFFFF));
        button.setCompoundDrawablePadding(16);
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        buttonParams.topMargin = instructionParams.topMargin + 90;
        button.setLayoutParams(buttonParams);
        qrContainer.addView(button);
        photoButton = button;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPickingImage = true;
                setLifecycleJsSuppressed(true);
                pickImageLauncher.launch("image/*");
            }
        });

        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, scanSide - 4);
        animation.setDuration(2000);
        animation.setRepeatCount(TranslateAnimation.INFINITE);
        animation.setRepeatMode(TranslateAnimation.RESTART);
        line.startAnimation(animation);
    }

    interface QRCodeResult {
        void onQRCode(String code);
    }

    private void setLifecycleJsSuppressed(boolean suppressed) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSuppressLifecycleJs(suppressed);
        }
    }
}
