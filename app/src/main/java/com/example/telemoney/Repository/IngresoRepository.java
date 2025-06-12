package com.example.telemoney.Repository;

import android.util.Log;

import com.example.telemoney.Model.Ingreso;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class IngresoRepository {
    private final FirebaseFirestore db;
   // private final FirebaseAuth auth;

    public IngresoRepository() {
        this.db = FirebaseFirestore.getInstance();
       // this.auth = FirebaseAuth.getInstance();
    }
    public void guardarIngreso(Ingreso ingreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("ingresos")
                .document(ingreso.getId())
                .set(ingreso)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    public void obtenerIngresos(Consumer<List<Ingreso>> callback, OnFailureListener failure) {
        db.collection("ingresos")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Ingreso> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Ingreso ingreso = doc.toObject(Ingreso.class);
                        lista.add(ingreso);
                    }
                    callback.accept(lista);
                })
                .addOnFailureListener(failure);
    }

    public void actualizarIngreso(Ingreso ingreso, OnSuccessListener<Void> success, OnFailureListener failure) {
        guardarIngreso(ingreso, success, failure); // misma lógica
    }

    public void eliminarIngreso(String ingresoId, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("ingresos")
                .document(ingresoId)
                .delete()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
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
        db.collection("ingresos")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String randomId = "ing_" + generarSufijoAleatorio(4); // 4 caracteres aleatorios
                    callback.accept(randomId);
                })
                .addOnFailureListener(failure);
    }


}

