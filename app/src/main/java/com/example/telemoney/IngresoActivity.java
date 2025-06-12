package com.example.telemoney;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.telemoney.Adpater.IngresoAdapter;
import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.Repository.IngresoRepository;
import com.example.telemoney.databinding.ActivityIngresoBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;

public class IngresoActivity extends AppCompatActivity {
    private ArrayList<Ingreso> listaIngresos;
    private IngresoRepository ingresoRepository;
    private IngresoAdapter adapter;
    ActivityIngresoBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIngresoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigation.setSelectedItemId(R.id.nav_ingresos);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_ingresos) {
                return true; // Ya estamos aquí
            } else if (itemId == R.id.nav_egresos) {
                startActivity(new Intent(this, EgresoActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_resumen) {
                startActivity(new Intent(this, ResumenActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_logout) {
                new AlertDialog.Builder(this)
                        .setTitle("¿Cerrar sesión?")
                        .setMessage("¿Estás segura de que deseas cerrar sesión?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            AuthUI.getInstance()
                                    .signOut(this)
                                    .addOnCompleteListener(task -> {
                                        Log.d("Logout", "Sesión cerrada exitosamente");
                                        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        overridePendingTransition(0, 0);
                                        finish();
                                    });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            }

            return false;
        });

        // Inicializar botones
        binding.fabAddIngreso.setOnClickListener(view -> nuevoIngreso());
        // Reciclar la vista para los Insets
        configurarRecyclerView();
    }
    // Boton de Nuevo Ingreso
    private void nuevoIngreso() {
        Intent intent = new Intent(IngresoActivity.this, AddIngresoActivity.class);
        addIngresoLauncher.launch(intent);
    }
    private void configurarRecyclerView() {
        listaIngresos = new ArrayList<>();

        adapter = new IngresoAdapter(listaIngresos, new IngresoAdapter.OnIngresoClickListener() {
            @Override
            public void onEdit(Ingreso ingreso) {
                Intent intent = new Intent(IngresoActivity.this, EditIngresoActivity.class);
                intent.putExtra("ingreso", ingreso);
                editIngresoLauncher.launch(intent);
                Toast.makeText(IngresoActivity.this, "Editar: " + ingreso.getTitulo(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(Ingreso ingreso) {
                new AlertDialog.Builder(IngresoActivity.this)
                        .setTitle("¿Eliminar ingreso?")
                        .setMessage("¿Estás segura de que deseas eliminar \"" + ingreso.getTitulo() + "\"?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            ingresoRepository.eliminarIngreso(ingreso.getId(),
                                    unused -> {
                                        int index = listaIngresos.indexOf(ingreso);
                                        if (index != -1) {
                                            listaIngresos.remove(index);
                                            adapter.notifyItemRemoved(index);
                                        }
                                        Toast.makeText(IngresoActivity.this, "Ingreso eliminado", Toast.LENGTH_SHORT).show();
                                    },
                                    e -> Toast.makeText(IngresoActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }


        });
        binding.recyclerViewIngresos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewIngresos.setAdapter(adapter);


        // Carga los ingresos desde el repositorio
        ingresoRepository = new IngresoRepository(); // Asegúrate de tener esto como atributo o aquí
        ingresoRepository.obtenerIngresos(lista -> {
            listaIngresos.clear();
            listaIngresos.addAll(lista);
            adapter.notifyDataSetChanged();
            Log.d("IngresoActivity", "Ingresos cargados: " + lista.size());
        }, e -> {
            Log.e("IngresoActivity", "Error al cargar ingresos", e);
            Toast.makeText(this, "Error al cargar ingresos", Toast.LENGTH_SHORT).show();
        });
    }

    private final ActivityResultLauncher<Intent> addIngresoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.hasExtra("nuevo_ingreso")) {
                        Ingreso nuevoIngreso = (Ingreso) data.getSerializableExtra("nuevo_ingreso");

                        // Agregar el nuevo ingreso a la lista
                        listaIngresos.add(nuevoIngreso);
                        adapter.notifyItemInserted(listaIngresos.size() - 1);

                        Toast.makeText(this, "Nuevo ingreso agregado", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    Log.d("IngresoActivity", "Creación de ingreso cancelada");
                }
            });

    private final ActivityResultLauncher<Intent> editIngresoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.hasExtra("ingreso_actualizado")) {
                        Ingreso actualizado = (Ingreso) data.getSerializableExtra("ingreso_actualizado");

                        // Buscar por ID en lugar de usar indexOf (más seguro)
                        for (int i = 0; i < listaIngresos.size(); i++) {
                            if (listaIngresos.get(i).getId().equals(actualizado.getId())) {
                                listaIngresos.set(i, actualizado);
                                adapter.notifyItemChanged(i);
                                Toast.makeText(this, "Ingreso actualizado correctamente", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    // El usuario canceló la edición
                    Log.d("IngresoActivity", "Edición cancelada por el usuario");
                }
            });

}