package com.sgu.student.arcus;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.sgu.student.arcus.db.Database;
import com.sgu.student.arcus.db.entity.ClassesEntity;
import com.sgu.student.arcus.db.entity.MaterialsEntity;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MaterialsPage.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MaterialsPage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MaterialsPage extends Fragment implements AdapterView.OnItemSelectedListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private static final int fileExplorerRequest = 1;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private String selectedItem;
    private String file_path;
    private String device_name;
    NotificationManager mNotificationManager;


    //Google Nearby variables
    private String targetId;
    public static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String TAG = "ARCus connection status";
    private Button connectButton;
    private Button stopConnect;
    private Button advertiseButton;
    private Button stopAdvertise;

    private ConnectionsClient connectionsClient;

    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    private boolean mIsConnecting = false;
    private boolean mIsDiscovering = false;
    private boolean mIsAdvertising = false;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
    //until here

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public MaterialsPage() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MaterialsPage.
     */
    // TODO: Rename and change types and number of parameters
    public static MaterialsPage newInstance(String param1, String param2) {
        MaterialsPage fragment = new MaterialsPage();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_materials_page, container, false);

        ImageView reveal_add_button = v.findViewById(R.id.add_new_material_button);
        reveal_add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getView().findViewById(R.id.new_material_field).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.add_new_material_button).setVisibility(View.GONE);
                getView().findViewById(R.id.cancel_new_material_button).setVisibility(View.VISIBLE);
                }
        });
        ImageView cancel_add_button = v.findViewById(R.id.cancel_new_material_button);
        cancel_add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getView().findViewById(R.id.new_material_field).setVisibility(View.GONE);
                getView().findViewById(R.id.add_new_material_button).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.cancel_new_material_button).setVisibility(View.GONE);

            }
        });
        Button browse = v.findViewById(R.id.browse_button);
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
                getFile();

            }
        });

        Button confirm = v.findViewById(R.id.confirm_material_add);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMaterial();
            }
        });

        Spinner spinner = (Spinner) v.findViewById(R.id.materials_class_list);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // On selecting a spinner item
                selectedItem = parent.getItemAtPosition(position).toString();
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });
        Database database = Room.databaseBuilder(getContext(), Database.class, "mainDB").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        List<ClassesEntity> classesList = database.getClassesDao().getClassesEntityList();
        List<String> classesName = new ArrayList<>();
        for(ClassesEntity x : classesList){
            classesName.add(x.getName());
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, classesName);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);

        MaterialsEntity[] materials = database.getMaterialsDao().getMaterialsEntity();

        mRecyclerView = v.findViewById(R.id.materials_list);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        ArrayList<MaterialsEntity> dataset = new ArrayList<>();
        for(int i = 0;i<materials.length;i++){
            dataset.add(materials[i]);
        }
        mAdapter = new MaterialsListAdapter(this.getContext(),dataset, this);
        mRecyclerView.setAdapter(mAdapter);

        connectButton = v.findViewById(R.id.connect_button);
        stopConnect = v.findViewById(R.id.stop_connect_button);
        advertiseButton = v.findViewById(R.id.advertise_button);
        stopAdvertise = v.findViewById(R.id.stop_advertise_button);
        advertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginAdvertising();
            }
        });
        stopAdvertise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAdvertise();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginDiscovering();
            }
        });
        stopConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopDiscovery();
            }
        });
        connectionsClient = Nearby.getConnectionsClient(getContext());
        mNotificationManager = (NotificationManager)getContext().getSystemService( getContext().NOTIFICATION_SERVICE );
        return v;
    }

    public void saveMaterial(){
        TextView tv = getView().findViewById(R.id.new_material_name);
        Database database = Room.databaseBuilder(getContext(), Database.class, "mainDB").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        List<ClassesEntity> selectedClass = database.getClassesDao().retrieveByName(selectedItem);

        MaterialsEntity materials = new MaterialsEntity();
        materials.setTitle(tv.getText().toString());
        materials.setPath(file_path);
        materials.setClass_id(selectedClass.get(0).getC_id());
        database.getMaterialsDao().insert(materials);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        stopAdvertise();
        stopDiscovery();
        ft.detach(this).attach(this).commit();

    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == fileExplorerRequest) {
            if(ExFilePickerResult.getFromIntent(data)!=null){
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            Uri uri = data.getData();
                if (result != null && result.getCount() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < result.getCount(); i++) {
                        stringBuilder.append(result.getNames().get(i));
                        if (i < result.getCount() - 1) stringBuilder.append(", ");
                    }
                    file_path = result.getPath() + stringBuilder.toString();
                    TextView tv = getView().findViewById(R.id.new_material_file);
                    tv.setText(file_path);

                }
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        if (!hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void getFile(){
        ExFilePicker filePicker = new ExFilePicker();
        filePicker.start(this, fileExplorerRequest);
    }

    public void beginAdvertising(){
        startAdvertising();
        advertiseButton.setVisibility(View.GONE);
        stopAdvertise.setVisibility(View.VISIBLE);
    }

    public void beginDiscovering(){
        if (!hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
        startDiscovery();
        connectButton.setVisibility(View.GONE);
        stopConnect.setVisibility(View.VISIBLE);

    }

    //Permission handling
    /** Returns true if the app was granted all the permissions. Otherwise, returns false. */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    /** Handles user acceptance (or denial) of our permission request. */

    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getActivity(), R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                getActivity().finish();
                return;
            }
        }
        getActivity().recreate();
    }

    //Google Nearby functions
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    private void startDiscovery() {
        EditText temp = getView().findViewById(R.id.mtrl_nameInput);
        device_name = temp.getText().toString();
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        connectionsClient.startDiscovery(
                getActivity().getPackageName(), endpointDiscoveryCallback, new DiscoveryOptions(STRATEGY))
                .addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        onDiscoveryStarted();
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                logW("startDiscovering() failed.", e);
                                onDiscoveryFailed();
                            }
                        });
    }

    protected void onDiscoveryStarted() {}

    protected void onDiscoveryFailed() {}

    protected void onEndpointDiscovered(final Endpoint endpoint) {
        // We found an advertiser!
        if (!isConnecting()) {
            AlertDialog.Builder Abuilder = new AlertDialog.Builder(MaterialsPage.this.getContext());
            Abuilder.setCancelable(true);
            Abuilder.setTitle("Connecting");
            Abuilder.setMessage("Confirm connection request?\n Endpoint: "+endpoint.name);
            Abuilder.setPositiveButton("Confirm",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectToEndpoint(endpoint);
                        }
                    });
            Abuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MaterialsPage.this.getContext(), "Connection request to: "+endpoint.name+" refused", Toast.LENGTH_SHORT).show();
                }
            });

            AlertDialog dialog = Abuilder.create();
            dialog.show();
        }}

    private void stopDiscovery(){
        mIsDiscovering = false;
        disconnectFromAllEndpoints();
        connectionsClient.stopDiscovery();
        connectButton.setVisibility(View.VISIBLE);
        stopConnect.setVisibility(View.GONE);
        Toast.makeText(getActivity(), "No longer discovering", Toast.LENGTH_SHORT).show();
    }

    protected boolean isAdvertising() {
        return mIsAdvertising;
    }
    /** Broadcasts our presence using Nearby Connections so other players can find us. */
    private void startAdvertising() {
        mIsAdvertising = true;
        EditText temp = getView().findViewById(R.id.mtrl_nameInput);
        device_name = temp.getText().toString();
        connectionsClient.startAdvertising(
                device_name,getActivity().getPackageName(), connectionLifecycleCallback, new AdvertisingOptions(STRATEGY))
                .addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        logV("Now advertising endpoint " + device_name);
                        onAdvertisingStarted();
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                logW("startAdvertising() failed.", e);
                                onAdvertisingFailed();
                            }
                        });
    }

    protected void onAdvertisingStarted() {
    }

    protected void onAdvertisingFailed() {
        stopAdvertise.setVisibility(View.GONE);
        advertiseButton.setVisibility(View.VISIBLE);
    }

    private void stopAdvertise(){
        mIsAdvertising = false;
        disconnectFromAllEndpoints();
        connectionsClient.stopAdvertising();
        advertiseButton.setVisibility(View.VISIBLE);
        stopAdvertise.setVisibility(View.GONE);
        Toast.makeText(getActivity(), "All current connection severed. No longer advertising!", Toast.LENGTH_SHORT).show();
    }

    public void sendFile(File f, String s) throws FileNotFoundException, UnsupportedEncodingException {
        Payload fileName = Payload.fromBytes(s.getBytes("UTF-8"));
        Payload filePayload = Payload.fromFile(f);
        Set<String> endpoints = mEstablishedConnections.keySet();
        connectionsClient
                .sendPayload(new ArrayList<>(endpoints), fileName)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("sendPayload() failed.", e);
                            }
                        });
        connectionsClient
                .sendPayload(new ArrayList<>(endpoints), filePayload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("sendPayload() failed.", e);
                            }
                        });
    }

    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // A connection to another device has been initiated! We'll accept the connection immediately.
        acceptConnection(endpoint);
    }

    protected void connectToEndpoint(final Endpoint endpoint) {
        logV("Sending a connection request to endpoint " + endpoint);
        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true;

        // Ask to connect
        connectionsClient
                .requestConnection(device_name, endpoint.getId(), connectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("requestConnection() failed.", e);
                                mIsConnecting = false;
                                onConnectionFailed(endpoint);
                            }
                        });
    }

    protected final boolean isConnecting() {
        return mIsConnecting;
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }

    protected void acceptConnection(final Endpoint endpoint) {
        connectionsClient
                .acceptConnection(endpoint.getId(), payloadCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("acceptConnection() failed.", e);
                            }
                        });
    }

    protected void rejectConnection(Endpoint endpoint) {
        connectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("rejectConnection() failed.", e);
                            }
                        });
    }

    protected void disconnect(Endpoint endpoint) {
        connectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }

    protected void disconnectFromAllEndpoints() {
        for (Endpoint endpoint : mEstablishedConnections.values()) {
            connectionsClient.disconnectFromEndpoint(endpoint.getId());
        }
        mEstablishedConnections.clear();
    }

    protected void stopAllEndpoints() {
        connectionsClient.stopAllEndpoints();
        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    Payload check;
    Payload incomingFile;
    String fileName;
    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();
                private final SimpleArrayMap<Long, NotificationCompat.Builder> incomingPayloads = new SimpleArrayMap<>();
                private final SimpleArrayMap<Long, NotificationCompat.Builder> outgoingPayloads = new SimpleArrayMap<>();

                private NotificationCompat.Builder buildNotification(Payload payload, boolean isIncoming) {
                    NotificationCompat.Builder notification = new NotificationCompat.Builder(getContext())
                            .setContentTitle(isIncoming ? "Receiving..." : "Sending...")
                            .setSmallIcon(R.drawable.file_notif_1);
                    int size = (int) payload.asFile().getSize();
                    boolean indeterminate = false;
                    if (size == -1) {
                        // This is a stream payload, so we don't know the size ahead of time.
                        size = 100;
                        indeterminate = true;
                    }
                    notification.setProgress(size, 0, indeterminate);
                    return notification;
                }

                @Override
                public void onPayloadReceived(String endpointId, final Payload payload) {
                    check = payload;
                    if (payload.getType() == Payload.Type.BYTES) {
                        String payloadFilenameMessage = null;
                        try {
                            payloadFilenameMessage = new String(payload.asBytes(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        addPayloadFilename(payloadFilenameMessage);

                    } else if (payload.getType() == Payload.Type.FILE) {
                        // Add this to our tracking map, so that we can retrieve the payload later.
                        incomingFile = payload;

                        // Build and start showing the notification.
                        NotificationCompat.Builder notification = buildNotification(payload, true /*isIncoming*/);
                        mNotificationManager.notify((int) payload.getId(), notification.build());

                        // Add it to the tracking list so we can update it.
                        incomingPayloads.put(payload.getId(), notification);
                    }
                }
                private void addPayloadFilename(String payloadFilenameMessage) {
                    fileName = payloadFilenameMessage;
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (check != null) {
                        if (check.getType() == Payload.Type.FILE) {
                            long payloadId = update.getPayloadId();
                            NotificationCompat.Builder notification;
                            notification = null;
                            if (incomingPayloads.containsKey(payloadId)) {
                                notification = incomingPayloads.get(payloadId);

                            } else if (outgoingPayloads.containsKey(payloadId)) {
                                notification = outgoingPayloads.get(payloadId);

                            }
                            switch (update.getStatus()) {
                                case PayloadTransferUpdate.Status.IN_PROGRESS:
                                    int size = (int) update.getTotalBytes();
                                    if (size == -1) {
                                        // This is a stream payload, so we don't need to update anything at this point.
                                        return;
                                    }
                                    notification.setProgress(size, (int) update.getBytesTransferred(), false /* indeterminate */);
                                    break;
                                case PayloadTransferUpdate.Status.SUCCESS:
                                    // SUCCESS always means that we transferred 100%.
                                    if (check != null) {
                                        if (check.getType() == Payload.Type.FILE) {
                                            Payload payload = incomingFile;
                                            if (payload.getType() == Payload.Type.FILE) {
                                                // Retrieve the filename that was received in a bytes payload.
                                                String newFilename = fileName;

                                                File payloadFile = payload.asFile().asJavaFile();
                                                // Rename the file.
                                                payloadFile.renameTo(new File(payloadFile.getParentFile(), newFilename));
                                                Toast.makeText(getActivity(), "Saved in: " + payloadFile.getPath(), Toast.LENGTH_LONG).show();
                                                notification
                                                        .setProgress(100, 100, false /* indeterminate */)
                                                        .setContentText("Transfer complete!");
                                                incomingPayloads.remove(payloadId);
                                                outgoingPayloads.remove(payloadId);
                                            }
                                        }
                                    }
                                    break;
                                case PayloadTransferUpdate.Status.FAILURE:
                                    notification
                                            .setProgress(0, 0, false)
                                            .setContentText("Transfer failed");
                                    break;
                            }

                            mNotificationManager.notify((int) payloadId, notification.build());
                        }
                    }
                }
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback(){
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.i(TAG, "onConnectionInitiated: accepting connection");
                    logD(
                            String.format(
                                    "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                                    endpointId, connectionInfo.getEndpointName()));
                    Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mPendingConnections.put(endpointId, endpoint);
                    MaterialsPage.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    logD(String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

                    // We're no longer connecting
                    mIsConnecting = false;

                    if (!result.getStatus().isSuccess()) {
                        logW(
                                String.format(
                                        "Connection failed. Received status %s.",
                                        MaterialsPage.toString(result.getStatus())));
                        onConnectionFailed(mPendingConnections.remove(endpointId));
                        return;
                    }
                    connectedToEndpoint(mPendingConnections.remove(endpointId));
                }

                @Override
                public void onDisconnected(String endpointId) {
                    if (!mEstablishedConnections.containsKey(endpointId)) {
                        logW("Unexpected disconnection from endpoint " + endpointId);
                        return;
                    }
                    disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
                }
            };

    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    logD(
                        String.format(
                                "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                endpointId, info.getServiceId(), info.getEndpointName()));


                        Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                        mDiscoveredEndpoints.put(endpointId, endpoint);
                        onEndpointDiscovered(endpoint);

                }

                @Override
                public void onEndpointLost(String endpointId) {
                    logD(String.format("onEndpointLost(endpointId=%s)", endpointId));
                }
            };

    protected void onConnectionFailed(Endpoint endpoint) {
        if(isDiscovering() && !getDiscoveredEndpoints().isEmpty()){
            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
        }
    }

    /** Called when someone has connected to us. Override this method to act on the event. */
    protected void onEndpointConnected(Endpoint endpoint) {
        TextView connections = getView().findViewById(R.id.mtrl_connectedName);
        connections.setText("");
        Set<Endpoint> sets = getConnectedEndpoints();
        Iterator<Endpoint> iterator = sets.iterator();
        while(iterator.hasNext()){
            connections.append(iterator.next().getName()+"; ");
        }
    }

    /** Called when someone has disconnected. Override this method to act on the event. */
    protected void onEndpointDisconnected(Endpoint endpoint) {
        TextView connections = getView().findViewById(R.id.mtrl_connectedName);
        connections.setText("");
        Set<Endpoint> sets = getConnectedEndpoints();
        Iterator<Endpoint> iterator = sets.iterator();
        while(iterator.hasNext()){
            connections.append(iterator.next().getName()+"; ");
        }

    }

    /** Returns a list of currently connected endpoints. */
    protected Set<Endpoint> getDiscoveredEndpoints() {
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.addAll(mDiscoveredEndpoints.values());
        return endpoints;
    }

    /** Returns a list of currently connected endpoints. */
    protected Set<Endpoint> getConnectedEndpoints() {
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.addAll(mEstablishedConnections.values());
        return endpoints;
    }

    @CallSuper
    protected void logV(String msg) {
        Log.v(TAG, msg);
    }

    @CallSuper
    protected void logD(String msg) {
        Log.d(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg) {
        Log.w(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg, Throwable e) {
        Log.w(TAG, msg, e);
    }

    @CallSuper
    protected void logE(String msg, Throwable e) {
        Log.e(TAG, msg, e);
    }

    @SuppressWarnings("unchecked")
    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
    }

    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

    /** Represents a device we can talk to. */
    protected static class Endpoint {
        @NonNull private final String id;
        @NonNull private final String name;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }

}
