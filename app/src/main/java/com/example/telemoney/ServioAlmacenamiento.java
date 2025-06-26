package com.example.telemoney;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.UUID;

public class ServioAlmacenamiento {
    private final FirebaseStorage storage;
    private final StorageReference storageRef;
    private static final String TAG = "ServioAlmacenamiento";

    public ServioAlmacenamiento() {
        this.storage = FirebaseStorage.getInstance();
        this.storageRef = storage.getReference();
        Log.d(TAG, "ServioAlmacenamiento inicializado");
    }
    public void conectarServicio() {
        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference reference = storage.getReference();
            Log.d(TAG, "Conexión con Firebase Storage establecida");
        } catch (Exception e) {
            Log.e(TAG, "Error al conectar con Firebase Storage", e);
            throw new RuntimeException("Error al conectar con Firebase Storage: " + e.getMessage());
        }
    }
    // Guardar comprobante de ingresos/egresos en una ruta específica
    public void guardarArchivo(Uri fileUri, String nombre, OnCompleteListener<UploadTask.TaskSnapshot> listener) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("comprobantes").child(nombre);
        ref.putFile(fileUri).addOnCompleteListener(listener);
    }
    // Obtener comprobante de ingresos/egresos en una ruta específica
    public void obtenerArchivo(String nombre, OnSuccessListener<Uri> success, OnFailureListener failure) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("comprobantes").child(nombre);
        ref.getDownloadUrl().addOnSuccessListener(success).addOnFailureListener(failure);
    }
    // Eliminar comprobante de ingresos/egresos en una ruta específica
    /*
    * Este metodo no era obligatorio, pero lo hice para que no se me sature de imagenes en el storage
    * asi que cuando se borre un ingreso o egreso, tambien se deberia borrar su imagen correspondiente
    * */
    public void eliminarArchivo(String nombre, OnSuccessListener<Void> success, OnFailureListener failure) {
        StorageReference ref = storage.getReference().child("comprobantes").child(nombre);
        ref.delete().addOnSuccessListener(success).addOnFailureListener(failure);
    }
}
