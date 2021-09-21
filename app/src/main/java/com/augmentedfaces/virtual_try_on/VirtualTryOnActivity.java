package com.augmentedfaces.virtual_try_on;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.augmentedfaces.virtual_try_on.Adapters.Models_Adapter;
import com.augmentedfaces.virtual_try_on.utils.RecyclerTouchListener;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import com.google.ar.sceneform.math.*;
import android.os.Handler;

import android.widget.Button;
import android.graphics.Color;
import android.content.res.*;

public class VirtualTryOnActivity extends AppCompatActivity {

    private static final String TAG = VirtualTryOnActivity.class.getSimpleName();

    private static final double MIN_OPENGL_VERSION = 3.0;

    private ModelRenderable faceRegionsRenderable;

    Scene scene;
    ArSceneView sceneView;
    AugmentedFaceNode faceNode;

    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();
    Button color, color2;
    boolean firstColorButtonclicked = true;
    SeekBar seek;

    Models_Adapter mAdapter;
    RecyclerView recyclerView;
    int image_position = 0;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_virtual_try);

        FaceArFragment arFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.face_fragment);
        color = findViewById(R.id.button);
        color2 = findViewById(R.id.button2);
        seek = findViewById(R.id.seekBar);

        HashMap<Integer, Boolean> colorButtonState = new HashMap<>();//save which color was chosen for each glass
        HashMap<Integer, Integer> SeekbarState = new HashMap<>();//save the seekbar progress state for each glass

        color.setOnClickListener(v -> {
            firstColorButtonclicked = true;
            augmfacDisplay(image_position, firstColorButtonclicked);
            colorButtonState.put(image_position, firstColorButtonclicked);
        });
        color2.setOnClickListener(v -> {
            firstColorButtonclicked = false;
            augmfacDisplay(image_position, firstColorButtonclicked);
            colorButtonState.put(image_position, firstColorButtonclicked);
        });

        color.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(94, 136 , 168)));
        color2.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(139, 0 , 0)));
        seek.setProgress(1);
        int seekBarProgress = seek.getProgress();

        recyclerView = findViewById(R.id.models_recy);

        ArrayList<Integer> models = new ArrayList<>();
        models.add(R.drawable.gl2); models.add(R.drawable.fdf); models.add(R.drawable.gr); models.add(R.drawable.am_body);

        for (int i=0; i<models.size(); i++) {
            colorButtonState.put(i, firstColorButtonclicked);
            SeekbarState.put(i, seekBarProgress);
        }

        augmfacDisplay(image_position, Objects.requireNonNull(colorButtonState.get(image_position)));

        mAdapter = new Models_Adapter(this, models);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                image_position = position;
                augmfacDisplay(position, Objects.requireNonNull(colorButtonState.get(position)));
                if (position == 0) {
                    color.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(94, 136 , 168)));
                    color2.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(139, 0 , 0)));
                }
                else if (position == 1) {
                    color2.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(255, 165 , 0)));
                    color.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(0, 0 , 0)));
                }
                else if (position == 2) {
                    color2.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(0, 0 , 0)));
                    color.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(50, 206 , 50)));
                }
                else if (position == 3) {
                    color.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(240, 240 , 23)));
                    color2.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(49, 49 , 178)));
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ChangeScale(progress);
                SeekbarState.put(image_position, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        assert arFragment != null;
        sceneView = arFragment.getArSceneView();

        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        sceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);

        scene = sceneView.getScene();

        scene.addOnUpdateListener(
                (FrameTime frameTime) -> {
                    if (faceRegionsRenderable == null) {
                        return;
                    }

                    Collection<AugmentedFace> faceList =
                            Objects.requireNonNull(sceneView.getSession()).getAllTrackables(AugmentedFace.class);


                    // Make new AugmentedFaceNodes for any new faces.
                    for (AugmentedFace face : faceList) {


                        if (!faceNodeMap.containsKey(face)) {

                            faceNode = new AugmentedFaceNode(face);

                            seek.setProgress(Objects.requireNonNull(SeekbarState.get(image_position)));
                            ChangeScale(Objects.requireNonNull(SeekbarState.get(image_position)));

                            faceNode.setParent(scene);
                            faceNode.setFaceRegionsRenderable(faceRegionsRenderable);
                            faceNodeMap.put(face, faceNode);

                        }
                    }



                    // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                    Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter =
                            faceNodeMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iter.next();
                        AugmentedFace face = entry.getKey();
                        if (face.getTrackingState() == TrackingState.STOPPED) {
                            AugmentedFaceNode faceNode = entry.getValue();
                            faceNode.setParent(null);
                            iter.remove();
                        }
                    }
                });
    }

    public void augmfacDisplay(int position, boolean firstColorButtonclicked){
        int model;

        if (position == 0)
            model = R.raw.glasses;
        else if (position == 1)
            model = R.raw.goldallo;
        else if (position == 2)
            model = R.raw.sunglasses_01;
        else
            model = R.raw.am_body;

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            faceNodeMap.clear();

            try{
                faceNode.setParent(null);
            }
            catch (Exception e){
                e.printStackTrace();
                Log.e(TAG, "faceNode exception");
            }
        }, 120);

        // Load the face regions renderable.
        // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.
        ModelRenderable.builder()
                .setSource(this, model)
                .build()
                .thenAccept(
                        modelRenderable -> {

                            faceRegionsRenderable = modelRenderable;

                            //giving color to glasses
                            if (!firstColorButtonclicked) {
                                switch (position) {
                                    case 0:
                                        faceRegionsRenderable.getMaterial().setFloat3("baseColorTint", 0.139f, 0, 0);
                                        break;
                                    case 1:
                                        faceRegionsRenderable.getMaterial(0).setFloat3("baseColorTint", 0.0f, 0.0f, 0.0f);
                                        faceRegionsRenderable.getMaterial(1).setFloat3("baseColorTint", 0.0f, 0.0f, 0.0f);
                                        faceRegionsRenderable.getMaterial(2).setFloat3("baseColorTint", 0.255f, 0.165f, 0);
                                        break;
                                    case 2:
                                        faceRegionsRenderable.getMaterial(0).setFloat3("baseColorTint", 0, 0, 0);
                                        faceRegionsRenderable.getMaterial(1).setFloat3("baseColorTint", 0, 0, 0);
                                        break;
                                    case 3:
                                        faceRegionsRenderable.getMaterial().setFloat3("baseColor", 0, 0, 0.255f);
                                        break;
                                }
                            }


                            modelRenderable.setShadowCaster(false);
                            modelRenderable.setShadowReceiver(false);
                        });

    }

    public void ChangeScale(int progress) {
        switch (progress) {
            //changing glass object scale
            case 0:
                faceNode.setLocalScale(new Vector3(0.85f, 0.7f, 0.7f));
                break;
            case 1:
                faceNode.setLocalScale(new Vector3(1f, 0.85f, 0.85f));
                break;
            case 2:
                faceNode.setLocalScale(new Vector3(1f, 1f, 1f));
                break;
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (ArCoreApk.getInstance().checkAvailability(activity)
                == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(TAG, "Augmented Faces requires ARCore.");
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
