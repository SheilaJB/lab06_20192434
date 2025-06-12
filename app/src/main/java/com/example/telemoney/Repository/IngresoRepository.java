package com.example.telemoney.Repository;

import android.util.Log;

import com.example.telemoney.Model.Ingreso;
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

public class IngresoRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public IngresoRepository() {
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
    public void guardarIngreso(Ingreso ingreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("IngresoRepository", "Guardando ingreso para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("ingresos")
                    .document(ingreso.getId())
                    .set(ingreso)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("IngresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }

    public void obtenerIngresos(Consumer<List<Ingreso>> callback, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("IngresoRepository", "Obteniendo ingresos para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("ingresos")
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Ingreso> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                Ingreso ingreso = doc.toObject(Ingreso.class);
                                lista.add(ingreso);
                            } catch (Exception e) {
                                Log.e("IngresoRepository", "Error convirtiendo documento: " + doc.getId(), e);
                            }
                        }
                        Log.d("IngresoRepository", "Ingresos encontrados: " + lista.size());
                        callback.accept(lista);
                    })
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("IngresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }

    public void actualizarIngreso(Ingreso ingreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        guardarIngreso(ingreso, success, failure);
    }

    public void eliminarIngreso(String ingresoId, OnSuccessListener<Void> success, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("IngresoRepository", "Eliminando ingreso " + ingresoId + " para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("ingresos")
                    .document(ingresoId)
                    .delete()
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("IngresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }
    public void obtenerIngresosPorMes(String fechaInicio, String fechaFin,
                                      Consumer<List<Ingreso>> callback,
                                      OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();
            Log.d("IngresoRepository", "Obteniendo ingresos del " + fechaInicio + " al " + fechaFin + " para usuario: " + uid);

            db.collection("usuarios")
                    .document(uid)
                    .collection("ingresos")
                    .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                    .whereLessThanOrEqualTo("fecha", fechaFin)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Ingreso> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                Ingreso ingreso = doc.toObject(Ingreso.class);
                                lista.add(ingreso);
                            } catch (Exception e) {
                                Log.e("IngresoRepository", "Error convirtiendo documento: " + doc.getId(), e);
                            }
                        }
                        Log.d("IngresoRepository", "Ingresos del período encontrados: " + lista.size());
                        callback.accept(lista);
                    })
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("IngresoRepository", "Error: Usuario no autenticado", e);
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

    public void generarNuevoId(Consumer<String> callback, OnFailureListener failure) {
        try {
            String uid = obtenerUidUsuario();

            db.collection("usuarios")
                    .document(uid)
                    .collection("ingresos")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String randomId = "ing_" + uid.substring(0, Math.min(uid.length(), 6)) + "_" + generarSufijoAleatorio(4);
                        callback.accept(randomId);
                    })
                    .addOnFailureListener(failure);
        } catch (IllegalStateException e) {
            Log.e("IngresoRepository", "Error: Usuario no autenticado", e);
            failure.onFailure(e);
        }
    }
    public boolean isUsuarioAutenticado() {
        return auth.getCurrentUser() != null;
    }
    public String obtenerEmailUsuario() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser != null ? currentUser.getEmail() : null;
    }

}

