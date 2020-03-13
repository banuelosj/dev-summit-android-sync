# Sync Offline Edits and Query Replica Id's on the server using the Google Volley library
From the ArcGIS Runtime SDK for Android Sync Issues Developer Summit 2020 Presentation

![Sync and Query](https://github.com/banuelosj/dev-summit-android-sync/blob/master/sync-query-replica.png)

## How to use the sample
1. Click on the bottom right `FloatingActionButton` to load the local replica. 
2. Once the data loads, you can click on the top-most blue `FloatingActionButton` that appears to query the service in order to check if the replica exists on the server.
3. The middle `FloatingActionButton` allows you add new points onto the `ArcGISMap` and onto the local replica.
4. The bottommost `FloatingActionButton` will allow you sync the data using the `GeodatabaseSyncTask`.
5. The bottommost button will send a request to register the replica from the device.


## How to obtain the data
1. Create a replica from the [Sync/SaveTheBaySync](https://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/SaveTheBaySync/FeatureServer) feature service.
2. Use the `Create Replica` operation of the [feature service](https://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/SaveTheBaySync/FeatureServer/createReplica).
3. Provide a name for the `Replica Name:`.
4. Provide the value `1` for the `Layers` property to use the [Birds](https://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/SaveTheBaySync/FeatureServer/1) layer.
5. Enter the following value for the `Geometry` property:
* `{"xmin":-121.19430541992189,"ymin":-120.27420043945311,"xmax":34.883114642235185,"ymax":35.34033442007605}`
6. The `Input Spatial Reference` value will be `4326`.
7. For the `Sync Direction` property, use `bidirectional` if you would like to download and upload changes from the mobile geodatabase on the device.
8. Click on the `Create Replica` button to generate the mobile geodatabase
9. Copy the url provided after successfully generating the replica onto a new tab to download the `.geodatabase` file.
10. Rename the file for a more readable name (will use baySummit.geodatabase in the next steps), as it will be used to side-load the file onto the Android device.

## Side-load the data onto the Android device
1. You can use the [Android Studio Device File Explorer](https://developer.android.com/studio/debug/device-file-explorer) to upload the file onto the device directly from Android Studio.
3. If using the terminal to side-load the data follow the steps below.
2. `Side-load` the mobile geodatabase obtained from the steps above onto an Android Studio emulator or physical Android device.
3. Create an ArcGIS/geodatabases folder on your device. You can use the [Android Debug Bridge (adb)](https://developer.android.com/guide/developing/tools/adb.html) tool found in **<sdk-dir>/platform-tools**.
4. Open up a command prompt and execute the ```adb shell``` command to start a remote shell on your target device.
5. Navigate to your sdcard directory, e.g. ```cd /sdcard/```.  
6. Create the ArcGIS/samples/TileCache directory, ```mkdir ArcGIS/geodatabases```.
7. You should now have the following directory on your target device, ```/sdcard/ArcGIS/geodatabases```. We will copy the contents of the downloaded data into this directory. Note:  Directory may be slightly different on your device.
8. Exit the shell with the, ```exit``` command.
9. While still in your command prompt, navigate to the folder where you extracted the contents of the data from step 4 and execute the following command: 
	* ```adb push baySummit.geodatabase /sdcard/ArcGIS/geodatabases``` 

## Relevant API
* FeatureLayer
* FeatureTable
* Geodatabase
* GeodatabaseFeatureTable
* GeodatabaseSyncTask
* SyncGeodatabaseJob
* SyncGeodatabaseParameters
* [Google Volley](https://developer.android.com/training/volley)
