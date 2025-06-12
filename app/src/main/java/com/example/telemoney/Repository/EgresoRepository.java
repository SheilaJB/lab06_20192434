package com.example.telemoney.Repository;

import android.util.Log;

import com.example.telemoney.Model.Egreso;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EgresoRepository {
    private final FirebaseFirestore db;

    public EgresoRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void guardarEgreso(Egreso egreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("egresos")
                .document(egreso.getId())
                .set(egreso)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    public void obtenerEgresos(Consumer<List<Egreso>> callback, OnFailureListener failure) {
        db.collection("egresos")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Egreso> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Egreso egreso = doc.toObject(Egreso.class);
                        lista.add(egreso);
                    }
                    callback.accept(lista);
                })
                .addOnFailureListener(failure);
    }

    public void actualizarEgreso(Egreso egreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        guardarEgreso(egreso, success, failure);
    }

    public void eliminarEgreso(String egresoId, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("egresos")
                .document(egresoId)
                .delete()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    public void generarNuevoId(Consumer<String> callback, OnFailureListener failure) {
        db.collection("egresos")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size() + 1;
                    String id = "eg_" + count;
                    callback.accept(id);
                })
                .addOnFailureListener(failure);
    }

}
