package com.example.telemoney;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.telemoney.Adpater.EgresoAdapter;
import com.example.telemoney.Model.Egreso;
import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.Repository.EgresoRepository;
import com.example.telemoney.databinding.ActivityEgresoBinding;

import java.util.ArrayList;

public class EgresoActivity extends AppCompatActivity {
    ActivityEgresoBinding binding;
    private final static String TAG = "EgresoActivity";
    private EgresoRepository egresoRepository;
    private ArrayList<Egreso> listaEgresos;
    private EgresoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEgresoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.bottomNavigation.setSelectedItemId(R.id.nav_egresos);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_egresos) {
                return true; // Ya estás aquí
            } else if (itemId == R.id.nav_ingresos) {
                startActivity(new Intent(this, IngresoActivity.class));
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
                            // Aquí puedes agregar lógica de logout real si tienes FirebaseAuth, etc.
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                            finish();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            }


            return false;
        });
        // Inicializar botones
        binding.fabAddEgreso.setOnClickListener(view -> nuevoEgreso());
        // Reciclar la vista para los Insets
        configurarRecyclerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.barra_menu, menu);
        return true;
    }

    private void nuevoEgreso() {
        Intent intent = new Intent(EgresoActivity.this, AddEgresoActivity.class);
        addEgresoLauncher.launch(intent);  // Usar launcher en lugar de startActivity
    }

    private void configurarRecyclerView(){
        listaEgresos = new ArrayList<>();

        adapter = new EgresoAdapter(listaEgresos, new EgresoAdapter.OnEgresoClickListener() {
            @Override
            public void onEdit(Egreso egreso) {
                Intent intent = new Intent(EgresoActivity.this, EditEgresoActivity.class);
                intent.putExtra("egreso", egreso);
                editEgresoLauncher.launch(intent);
                Toast.makeText(EgresoActivity.this, "Editar: " + egreso.getTitulo(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(Egreso egreso) {
                new AlertDialog.Builder(EgresoActivity.this)
                        .setTitle("¿Eliminar ingreso?")
                        .setMessage("¿Estás segura de que deseas eliminar \"" + egreso.getTitulo() + "\"?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            egresoRepository.eliminarEgreso(egreso.getId(),
                                    unused -> {
                                        int index = listaEgresos.indexOf(egreso);
                                        if (index != -1) {
                                            listaEgresos.remove(index);
                                            adapter.notifyItemRemoved(index);
                                        }
                                        Toast.makeText(EgresoActivity.this, "Ingreso eliminado", Toast.LENGTH_SHORT).show();
                                    },
                                    e -> Toast.makeText(EgresoActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
        binding.recyclerViewEgresos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewEgresos.setAdapter(adapter);

        // Cargar los egresos desde el repositorio
        egresoRepository = new EgresoRepository();
        egresoRepository.obtenerEgresos(
                egresos -> {
                    listaEgresos.clear();
                    listaEgresos.addAll(egresos);
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Egresos cargados: " + egresos.size());
                }, e -> {
                    Log.e(TAG, "Error al cargar egresos", e);
                    Toast.makeText(this, "Error al cargar egresos", Toast.LENGTH_SHORT).show();
        });
    }
    private final ActivityResultLauncher<Intent> addEgresoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.hasExtra("nuevo_egreso")) {
                        Egreso nuevoEgreso = (Egreso) data.getSerializableExtra("nuevo_egreso");

                        listaEgresos.add(nuevoEgreso);
                        adapter.notifyItemInserted(listaEgresos.size() - 1);

                        Toast.makeText(this, "Nuevo egreso agregado", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    Log.d("EgresoActivity", "Creación de egreso cancelada");
                }
            });


    private final ActivityResultLauncher<Intent> editEgresoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.hasExtra("egreso_actualizado")) {
                        Egreso actualizado = (Egreso) data.getSerializableExtra("egreso_actualizado");

                        // Buscar por ID en lugar de indexOf
                        for (int i = 0; i < listaEgresos.size(); i++) {
                            if (listaEgresos.get(i).getId().equals(actualizado.getId())) {
                                listaEgresos.set(i, actualizado);
                                adapter.notifyItemChanged(i);
                                Toast.makeText(this, "Egreso actualizado correctamente", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    Log.d("EgresoActivity", "Edición cancelada por el usuario");
                }
            });

}

