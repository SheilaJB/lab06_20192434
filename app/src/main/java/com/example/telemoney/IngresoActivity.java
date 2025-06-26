package com.example.telemoney;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemoney.Adpater.IngresoAdapter;
import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.Repository.IngresoRepository;
import com.example.telemoney.databinding.ActivityIngresoBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
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
        configurarSwipe();
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
            public void onDownload(Ingreso ingreso) {
                if (ingreso.getComprobanteUrl() == null || ingreso.getComprobanteUrl().isEmpty()) {
                    Toast.makeText(IngresoActivity.this, "No hay comprobante para descargar", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(IngresoActivity.this)
                        .setTitle("¿Descargar comprobante?")
                        .setMessage("¿Deseas descargar el comprobante asociado a \"" + ingreso.getTitulo() + "\"?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            // Verifica permisos si estás en Android 6–9
                           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                                return;
                            }*/

                            String nombreArchivo = "comprobante_" + ingreso.getId() + ".jpg";
                            File directorioDescargas = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File archivoDestino = new File(directorioDescargas, nombreArchivo);
                            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(ingreso.getComprobanteUrl());

                            storageRef.getFile(archivoDestino)
                                    .addOnSuccessListener(taskSnapshot -> {
                                        Toast.makeText(IngresoActivity.this,
                                                "Comprobante guardado en Descargas: " + nombreArchivo,
                                                Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(IngresoActivity.this, "Error al descargar comprobante", Toast.LENGTH_SHORT).show();
                                        Log.e("Descarga", "Error al descargar imagen", e);
                                    });
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
    private void configurarSwipe() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Ingreso ingreso = listaIngresos.get(position);

                new AlertDialog.Builder(IngresoActivity.this)
                        .setTitle("¿Eliminar ingreso?")
                        .setMessage("¿Estás segura de que deseas eliminar \"" + ingreso.getTitulo() + "\" y su comprobante?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            ingresoRepository.eliminarIngreso(ingreso.getId(),
                                    unused -> {
                                        // ✅ Eliminar comprobante si existe
                                        if (ingreso.getComprobanteNombre() != null && !ingreso.getComprobanteNombre().isEmpty()) {
                                            new ServioAlmacenamiento().eliminarArchivo(
                                                    ingreso.getComprobanteNombre(),
                                                    success -> Log.d("Storage", "Comprobante eliminado"),
                                                    error -> Log.e("Storage", "Error al eliminar comprobante", error)
                                            );
                                        }

                                        listaIngresos.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        Toast.makeText(IngresoActivity.this, "Ingreso eliminado", Toast.LENGTH_SHORT).show();
                                    },
                                    e -> {
                                        Toast.makeText(IngresoActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(position); // Restaurar swipe
                                    });
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position))
                        .show();
            }
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;
                Paint paint = new Paint();

                // Fondo rojo con bordes redondeados
                paint.setColor(ContextCompat.getColor(IngresoActivity.this, android.R.color.holo_red_dark));
                RectF background = new RectF(itemView.getRight() + dX, itemView.getTop() + 8,
                        itemView.getRight() - 8, itemView.getBottom() - 8);
                c.drawRoundRect(background, 24, 24, paint);

                // Ícono de eliminar centrado
                Drawable deleteIcon = ContextCompat.getDrawable(IngresoActivity.this, R.drawable.ic_delete);
                if (deleteIcon != null) {
                    int iconSize = 80;
                    int iconMargin = (itemView.getHeight() - iconSize) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconRight = itemView.getRight() - iconMargin - 16;

                    deleteIcon.setBounds(iconRight - iconSize, iconTop, iconRight, iconTop + iconSize);
                    deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    deleteIcon.draw(c);
                }

                // Efecto de transparencia suave
                float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth() * 0.3f;
                itemView.setAlpha(Math.max(alpha, 0.7f));

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewIngresos);
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