package com.example.questionnairelibrary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.questionnairelibrary.env.ImageUtils;
import com.example.questionnairelibrary.env.Logger;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stepstone.stepper.StepperLayout;

import java.nio.ByteBuffer;

public abstract class CameraActivity extends AppCompatActivity
        implements ImageReader.OnImageAvailableListener,
        Camera.PreviewCallback {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private boolean debug = false;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private int currentQuestionIdx = 0;
    private LinearLayout bottomSheetLayout;
    private LinearLayout buttonLike;
    private LinearLayout buttonDislike;
    private LinearLayout buttonBack;
    private LinearLayout buttonNext;
    private TextView tvQuestion;
    //    private LinearLayout gestureLayout;
//    private BottomSheetBehavior<LinearLayout> sheetBehavior;
//    protected ImageView bottomSheetArrowImageView;
//    private ImageView plusImageView, minusImageView;
//    private SwitchCompat apiSwitchCompat;
//    private TextView threadsTextView;
    private StepperLayout mStepperLayout;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.stepper_layout);

        mStepperLayout = findViewById(R.id.stepperLayout);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

//        threadsTextView = findViewById(R.id.threads);
//        plusImageView = findViewById(R.id.plus);
//        minusImageView = findViewById(R.id.minus);
//        apiSwitchCompat = findViewById(R.id.api_info_switch);
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        buttonLike = findViewById(R.id.ibLike);
        buttonDislike = findViewById(R.id.ibDislike);
        buttonBack = findViewById(R.id.ibBack);
        buttonNext = findViewById(R.id.ibNext);
        tvQuestion = findViewById(R.id.tvQuestion);
//        gestureLayout = findViewById(R.id.gesture_layout);
//        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
//        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
        ViewGroup.LayoutParams params = bottomSheetLayout.getLayoutParams();
        params.height = getResources().getDisplayMetrics().heightPixels / 4;
        bottomSheetLayout.setLayoutParams(params);
        tvQuestion.setHeight(getResources().getDisplayMetrics().heightPixels / 4 * 7 / 10);
        String[] strQuestions = {"Have you experienced any of the following symptoms in the past 48 hours:\n Fever or chills, cough, shortness of breath or difficulty breathing, fatigue, muscle or body aches, headache, new loss of taste or smell, sore throat, congestion or runny nose, nausea orvomiting, diarrhea",
                "Within the past 14 days, have you been in close physical contact (6 feet or closer for a cumulative total of 15 minutes) with:\nAnyone who is known to have laboratory-confirmed COVID-19?]\nOR\nAnyone who has any symptoms consistent with COVID-19?",
                "Are you isolating or quarantining because you may have been exposed to a person with COVID-19 or are worried that you may be sick with COVID-19?",
                "Are you currently waiting on the results of a COVID-19 test?"
        };
        tvQuestion.setText(strQuestions[currentQuestionIdx]);
        mStepperLayout.setAdapter(new MyStepperAdapter(getSupportFragmentManager(), this, strQuestions.length));

        buttonLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuestionIdx < strQuestions.length - 1) {
                    currentQuestionIdx++;
                    mStepperLayout.proceed();
                    tvQuestion.setText(strQuestions[currentQuestionIdx]);
                } else {
                    Toast.makeText(CameraActivity.this, "Complete ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonDislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuestionIdx < strQuestions.length - 1) {
                    currentQuestionIdx++;
                    mStepperLayout.proceed();
                    tvQuestion.setText(strQuestions[currentQuestionIdx]);
                } else {
                    Toast.makeText(CameraActivity.this, "Complete ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuestionIdx < strQuestions.length - 1) {
                    currentQuestionIdx++;
                    mStepperLayout.proceed();
                    tvQuestion.setText(strQuestions[currentQuestionIdx]);
                } else {
                    Toast.makeText(CameraActivity.this, "No more to next ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuestionIdx > 0) {
                    currentQuestionIdx--;
                    tvQuestion.setText(strQuestions[currentQuestionIdx]);
                    mStepperLayout.onBackClicked();
                } else {
                    Toast.makeText(CameraActivity.this, "No more to back ", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(
//                new ViewTreeObserver.OnGlobalLayoutListener() {
//                    @Override
//                    public void onGlobalLayout() {
//                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
//                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                        } else {
//                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                        }
//                        //                int width = bottomSheetLayout.getMeasuredWidth();
//                        int height = gestureLayout.getMeasuredHeight();
//
//                        sheetBehavior.setPeekHeight(height);
//                    }
//                });
//        sheetBehavior.setHideable(false);
//
//        sheetBehavior.setBottomSheetCallback(
//                new BottomSheetBehavior.BottomSheetCallback() {
//                    @Override
//                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
//                        switch (newState) {
//                            case BottomSheetBehavior.STATE_HIDDEN:
//                                break;
//                            case BottomSheetBehavior.STATE_EXPANDED:
//                            {
//                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
//                            }
//                            break;
//                            case BottomSheetBehavior.STATE_COLLAPSED:
//                            {
//                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
//                            }
//                            break;
//                            case BottomSheetBehavior.STATE_DRAGGING:
//                                break;
//                            case BottomSheetBehavior.STATE_SETTLING:
//                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
//                                break;
//                        }
//                    }
//
//                    @Override
//                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
//                });

//        cropValueTextView = findViewById(R.id.crop_info);
//        inferenceTimeTextView = findViewById(R.id.inference_info);
//
//        apiSwitchCompat.setOnCheckedChangeListener(this);
//
//        plusImageView.setOnClickListener(this);
//        minusImageView.setOnClickListener(this);
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        CameraActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }


    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
//                useCamera2API =
//                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
//                                || isHardwareLevelSupported(
//                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                useCamera2API = true;

                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();
        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
//                                    previewHeight = size.getHeight();
//                                    previewWidth = size.getWidth();
                                    previewHeight = 480;
                                    previewWidth = 480;
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

//    @Override
//    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//        setUseNNAPI(isChecked);
//        if (isChecked) apiSwitchCompat.setText("NNAPI");
//        else apiSwitchCompat.setText("TFLITE");
//    }
//
//    @Override
//    public void onClick(View v) {
//        if (v.getId() == R.id.plus) {
//            String threads = threadsTextView.getText().toString().trim();
//            int numThreads = Integer.parseInt(threads);
//            if (numThreads >= 9) return;
//            numThreads++;
//            threadsTextView.setText(String.valueOf(numThreads));
//            setNumThreads(numThreads);
//        } else if (v.getId() == R.id.minus) {
//            String threads = threadsTextView.getText().toString().trim();
//            int numThreads = Integer.parseInt(threads);
//            if (numThreads == 1) {
//                return;
//            }
//            numThreads--;
//            threadsTextView.setText(String.valueOf(numThreads));
//            setNumThreads(numThreads);
//        }
//    }

//    protected void showFrameInfo(String frameInfo) {
//        frameValueTextView.setText(frameInfo);
//    }
//
//    protected void showCropInfo(String cropInfo) {
//        cropValueTextView.setText(cropInfo);
//    }
//
//    protected void showInference(String inferenceTime) {
//        inferenceTimeTextView.setText(inferenceTime);
//    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void setNumThreads(int numThreads);

    protected abstract void setUseNNAPI(boolean isChecked);
}
