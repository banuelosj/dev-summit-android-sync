package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.geodatabase.GeodatabaseSyncTask;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseJob;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseParameters;
import com.esri.arcgisruntime.tasks.geodatabase.SyncLayerOption;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private String TAG = "Main Activity......";
    private MapView mapView = null;
    private GraphicsOverlay graphicsOverlay;
    private String serviceUrl;

    private Geodatabase geodatabase = null;
    private GeodatabaseSyncTask geodatabaseSyncTask = null;
    private Envelope editingExtent;
    private FeatureLayer mFeatureLayer;

    private Button registerButton;
    private FloatingActionButton fab, fabRequest, fabSync, fabEdit;

    private ProgressDialog mProgressDialog;

    /** Extent that can be used to create the replica on the rest endpoint for the sampleserver6 service **/
    // {"xmin":-121.19430541992189,"ymin":-120.27420043945311,"xmax":34.883114642235185,"ymax":35.34033442007605}
    // warning: the sampleserver6 clears out the replica every other day sometimes, so may need to create a new one
    // on a daily basis for testing
    /** Extent that can be used to create the replica on the rest endpoint for the sampleserver6 service **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*** requesting permission from the device for storage access ***/
        String[] reqPermission = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE };
        int requestCode = 2;
        // For API level 23+ request permission at runtime
        if (ContextCompat.checkSelfPermission(MainActivity.this, reqPermission[0]) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(MainActivity.this, reqPermission, requestCode);
            Log.i(TAG,"requesting permission");
        }
        /*** requesting permission from the device for storage access ***/

        mapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.LIGHT_GRAY_CANVAS, 35.148547256450655, -120.71090698242189, 10);
        mapView.setMap(map);

        graphicsOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        String pathToGDB = Environment.getExternalStorageDirectory() + getResources().getString(R.string.localGDB);

        geodatabase = new Geodatabase(pathToGDB);

        serviceUrl = getString(R.string.serviceUrl);
        geodatabaseSyncTask = new GeodatabaseSyncTask(serviceUrl);

        //setting up the button
        registerButton = findViewById(R.id.registerButton);
        registerButton.setVisibility(View.GONE);
        registerButton.setOnClickListener(v ->{
            registerGDB();
            //show the edit button and hide the register button
            registerButton.setVisibility(View.GONE);
        });

        //setting up the floating action buttons
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v->{
            loadGDB();
            //show the register button
            registerButton.setVisibility(View.VISIBLE);
            fab.hide();
            fabRequest.show();
            fabSync.show();
            fabEdit.show();
        });

        fabRequest = findViewById(R.id.fabRequest);
        fabRequest.hide();
        fabRequest.setOnClickListener(v->{
            registeredGDBcheck();
        });

        fabSync = findViewById(R.id.fabSync);
        fabSync.hide();
        fabSync.setOnClickListener(v->{
            syncGeodatabase();
        });

        fabEdit = findViewById(R.id.fabEdit);
        fabEdit.hide();
        fabEdit.setOnClickListener(v->{
            startEditing();
        });

    }

    private void loadGDB(){
        geodatabase.loadAsync();
        geodatabase.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                if(geodatabase.getLoadStatus() == LoadStatus.LOADED){
                    Log.i(TAG, "geodatabase loaded");
                    List<GeodatabaseFeatureTable> gdbFeatureTables = geodatabase.getGeodatabaseFeatureTables();

                    for(GeodatabaseFeatureTable geodatabaseFeatureTable : gdbFeatureTables){
                        geodatabaseFeatureTable.loadAsync();
                        geodatabaseFeatureTable.addDoneLoadingListener(new Runnable() {
                            @Override
                            public void run() {
                                if(geodatabaseFeatureTable.getLoadStatus() == LoadStatus.LOADED){
                                    mapView.getMap().getOperationalLayers().add(new FeatureLayer(geodatabaseFeatureTable));
                                }
                            }
                        });
                    }
                }
            }
        });
    } //end of load gdb

    private void registerGDB(){
        geodatabaseSyncTask.loadAsync();

        geodatabaseSyncTask.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                //code to check the sync id (replica id)
                UUID replicaID = geodatabase.getSyncId();
                //check to see if there is a replicaID returned
                if(replicaID != null && replicaID.toString().length() > 0){
                    Log.i(TAG, "replicaID: " + replicaID.toString());
                }
                else{
                    Log.i(TAG, "no replica id exists");
                }
                try{
                    ListenableFuture<Void> registerGeodatabase = geodatabaseSyncTask.registerSyncEnabledGeodatabaseAsync(geodatabase);
                    registerGeodatabase.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "registering a new replica...");
                            Toast.makeText(getApplicationContext(), "Registered Replica successfully!", Toast.LENGTH_LONG).show();
                            //hide the button after registering to only register once
                            registerButton.setVisibility(View.GONE);

                            UUID replicaIDNew = geodatabase.getSyncId();
                            if(replicaIDNew != null && replicaIDNew.toString().length() > 0){
                                Log.i(TAG, "replicaIDNew: " + replicaIDNew.toString());
                            }
                            else{
                                Log.i(TAG, "no replica id exists");
                            }
                        }
                    });
                }catch (ArcGISRuntimeException ex){
                    Log.i(TAG, "failed to register with " + ex.getMessage());
                }
            }
        });
    }

    private void registeredGDBcheck(){
        UUID replicaUUID = geodatabase.getSyncId();
        //the replica uuid returns lowercase letters, but the letters on the server are all upper case
        String replicaID = replicaUUID.toString().toUpperCase();

        //instantiate the RequestQueue
        RequestQueue queue = Volley.newRequestQueue(this);

        try{
            String url = serviceUrl + "/replicas?f=json";
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(response.contains(replicaID)){
                                Toast.makeText(getApplicationContext(), "replica: " + replicaID + " exists on the Server!", Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(getApplicationContext(), "replica: " + replicaID + " does not exists on the Server!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i(TAG, "request failed with: " + error.getMessage());
                }
            });

            //add the request to the RequestQueue
            queue.add(stringRequest);
        }catch (Exception ex){
            Log.i(TAG, "failed to send request: " + ex.getMessage());
        }
    }

    private void syncGeodatabase(){
        SyncGeodatabaseParameters syncGeodatabaseParameters = new SyncGeodatabaseParameters();
        syncGeodatabaseParameters.setSyncDirection(SyncGeodatabaseParameters.SyncDirection.BIDIRECTIONAL);
        syncGeodatabaseParameters.setRollbackOnFailure(false); //false with archived data

        //grab every layer id for each feature table in the geodatabase, then add to the sync job
        for(GeodatabaseFeatureTable geodatabaseFeatureTable : geodatabase.getGeodatabaseFeatureTables()){
            long serviceLayerId = geodatabaseFeatureTable.getServiceLayerId();
            SyncLayerOption syncLayerOption = new SyncLayerOption(serviceLayerId);
            syncGeodatabaseParameters.getLayerOptions().add(syncLayerOption);
        }

        try{
            geodatabaseSyncTask.loadAsync();
            geodatabaseSyncTask.addDoneLoadingListener(new Runnable() {
                @Override
                public void run() {
                    final SyncGeodatabaseJob syncGeodatabaseJob = geodatabaseSyncTask
                            .syncGeodatabaseAsync(syncGeodatabaseParameters, geodatabase);

                    syncGeodatabaseJob.start();
                    createProgressDialog(syncGeodatabaseJob);

                    syncGeodatabaseJob.addJobDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            if(syncGeodatabaseJob.getStatus() == Job.Status.SUCCEEDED){
                                Log.i(TAG, "sync succeeded...");
                                Toast.makeText(getApplicationContext(), "Sync succeeded!", Toast.LENGTH_LONG).show();
                            }
                            else if(syncGeodatabaseJob.getStatus() == Job.Status.FAILED){
                                Log.i(TAG, "replica did not sync successfully: " + syncGeodatabaseJob.getError());
                                Log.i(TAG, "cause: " + syncGeodatabaseJob.getError().getCause());
                                Toast.makeText(getApplicationContext(), "Failed to sync: " + syncGeodatabaseJob.getError(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });

        }catch(Exception ex){
            Log.i(TAG, "failed to sync with: " + ex.getMessage());
        }
    }

    private void createProgressDialog(Job job) {

        ProgressDialog syncProgressDialog = new ProgressDialog(this);
        syncProgressDialog.setTitle("Sync Job");
        syncProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        syncProgressDialog.setCanceledOnTouchOutside(false);
        syncProgressDialog.show();

        job.addProgressChangedListener(() -> syncProgressDialog.setProgress(job.getProgress()));

        job.addJobDoneListener(syncProgressDialog::dismiss);
    }

    private void startEditing(){

        FeatureLayer editingLayer = (FeatureLayer) mapView.getMap().getOperationalLayers().get(0); //OG
        mFeatureLayer = editingLayer;
        GeodatabaseFeatureTable gdbFeatureTable = (GeodatabaseFeatureTable) editingLayer.getFeatureTable();
        Log.i(TAG, "editingLayer: " + editingLayer.getFeatureTable().getDisplayName());

        //display a boundary for the extent of the featureTable for editing
        final SimpleLineSymbol boundarySymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 4);
        editingExtent = geodatabase.getGenerateGeodatabaseExtent();
        int wkid = editingExtent.getSpatialReference().getWkid();

        Graphic boundary = new Graphic(editingExtent, boundarySymbol);
        graphicsOverlay.getGraphics().add(boundary);

        //setting the click listener to add new points
        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView){
            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                //crewate a point from where the user clicked
                android.graphics.Point point = new android.graphics.Point((int) event.getX(), (int) event.getY());

                //create a map point from the point
                Point mapPoint = mapView.screenToLocation(point);

                if(gdbFeatureTable.isEditable()){
                    addFeature(mapPoint, gdbFeatureTable);
                }
                else{
                    Log.i(TAG, "featureTable is not editable...");
                }

                return super.onSingleTapConfirmed(event);
            }
        });
    }

    /***
     * adding a point to the geodatabase feature table
     * using the following service for the attributes
     * https://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/SaveTheBaySync/FeatureServer/1
     */
    private void addFeature(Point mapPoint, GeodatabaseFeatureTable featureTable){
        //create the default attributes for the featurre
        Map<String, Object> attributes = new HashMap<>();
        // for sampleserver6
        attributes.put("type", 4);
        attributes.put("confirmed", 0);
        attributes.put("creator", "jb");
        attributes.put("editor", "jack");
        attributes.put("comments", "sync 11-15");
        attributes.put("submitted", null);

        //creates a new feature susing default attributes and the point
        Feature feature = featureTable.createFeature(attributes, mapPoint);

        //check if the feature can be added to the featureTable
        if(featureTable.canAdd()){
            //add the new feature to the feature table
            try{
                ListenableFuture<Void> featureAdded = featureTable.addFeatureAsync(feature);
                featureAdded.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            featureAdded.get();

                            Map<String, Object> attr = feature.getAttributes();
                            for(Map.Entry<String, Object> entry : attr.entrySet()){
                                Log.i(TAG, "Key=" + entry.getKey());
                                Log.i(TAG, "Value=" + entry.getValue());
                            }

                        }catch (InterruptedException ex){
                            Log.i(TAG, "interruption exception" + ex.getMessage());
                        }
                        catch (ExecutionException ex){
                            Log.i(TAG, "execution exception" + ex.getMessage());
                            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }catch (IllegalArgumentException ex){
                Log.i(TAG, "failed to add feature with: " + ex.getMessage());
            }

        }
    }

}
