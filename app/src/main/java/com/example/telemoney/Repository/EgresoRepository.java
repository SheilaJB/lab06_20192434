package com.example.telemoney.Repository;

import android.util.Log;

import com.example.telemoney.Model.Egreso;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class EgresoRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public EgresoRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    private String obtenerUidUsuario() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        throw new IllegalStateException("Usuario no autenticado");
    }
    public void guardarEgreso(Egreso egreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("EgresoRepository", "Guardando egreso para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("egresos")
                    .document(egreso.getId())
                    .set(egreso)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("EgresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }

    public void obtenerEgresos(Consumer<List<Egreso>> callback, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("EgresoRepository", "Obteniendo egresos para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("egresos")
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Egreso> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                Egreso egreso = doc.toObject(Egreso.class);
                                lista.add(egreso);
                            } catch (Exception e) {
                                Log.e("EgresoRepository", "Error convirtiendo documento: " + doc.getId(), e);
                            }
                        }
                        Log.d("EgresoRepository", "Egresos encontrados: " + lista.size());
                        callback.accept(lista);
                    })
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("EgresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }

    public void actualizarEgreso(Egreso egreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        guardarEgreso(egreso, success, failure);
    }

    public void eliminarEgreso(String egresoId, OnSuccessListener<Void> success, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("EgresoRepository", "Eliminando egreso " + egresoId + " para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("egresos")
                    .document(egresoId)
                    .delete()
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("EgresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }

    public void generarNuevoId(Consumer<String> callback, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();

            db.collection("usuarios")
                    .document(uid)
                    .collection("egresos")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String randomSuffix = generarSufijoAleatorio(4);
                        String id = "eg_" + uid.substring(0, Math.min(uid.length(), 6)) + "_" + randomSuffix;
                        callback.accept(id);
                    })
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("EgresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }

    public void obtenerEgresosPorMes(String fechaInicio, String fechaFin,
                                     Consumer<List<Egreso>> callback,
                                     OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("EgresoRepository", "Obteniendo egresos del " + fechaInicio + " al " + fechaFin + " para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("egresos")
                    .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                    .whereLessThanOrEqualTo("fecha", fechaFin)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Egreso> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                Egreso egreso = doc.toObject(Egreso.class);
                                lista.add(egreso);
                            } catch (Exception e) {
                                Log.e("EgresoRepository", "Error convirtiendo documento: " + doc.getId(), e);
                            }
                        }
                        Log.d("EgresoRepository", "Egresos del período encontrados: " + lista.size());
                        callback.accept(lista);
                    })
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("EgresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }

    private String generarSufijoAleatorio(int longitud) {
        String caracteres = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < longitud; i++) {
            int index = random.nextInt(caracteres.length());
            sb.append(caracteres.charAt(index));
        }

        return sb.toString();
    }
}
