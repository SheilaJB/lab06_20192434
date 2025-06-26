package com.example.telemoney;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemoney.Adpater.EgresoAdapter;
import com.example.telemoney.Model.Egreso;
import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.Repository.EgresoRepository;
import com.example.telemoney.databinding.ActivityEgresoBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
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
                return true;
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
            }else if (itemId == R.id.nav_logout) {
                new AlertDialog.Builder(this)
                        .setTitle("¿Cerrar sesión?")
                        .setMessage("¿Estás segura de que deseas cerrar sesión?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            AuthUI.getInstance().signOut(this);

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
        binding.fabAddEgreso.setOnClickListener(view -> nuevoEgreso());
        configurarRecyclerView();
        configurarSwipe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.barra_menu, menu);
        return true;
    }
    private void nuevoEgreso() {
        Intent intent = new Intent(EgresoActivity.this, AddEgresoActivity.class);
        addEgresoLauncher.launch(intent);
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
            public void onDownload(Egreso egreso) {
                new AlertDialog.Builder(EgresoActivity.this)
                        .setTitle("¿Descargar comprobante?")
                        .setMessage("¿Deseas descargar el comprobante asociado a \"" + egreso.getTitulo() + "\"?")
                        .setPositiveButton("Sí", (dialog, which) -> {

                            String nombreArchivo = "comprobante_" + egreso.getId() + ".jpg";
                            File directorioDescargas = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File archivoDestino = new File(directorioDescargas, nombreArchivo);

                            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(egreso.getComprobanteUrl());

                            storageRef.getFile(archivoDestino)
                                    .addOnSuccessListener(taskSnapshot -> {
                                        Toast.makeText(EgresoActivity.this,
                                                "Comprobante guardado en Descargas: " + nombreArchivo,
                                                Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(EgresoActivity.this, "Error al descargar comprobante", Toast.LENGTH_SHORT).show();
                                        Log.e("Descarga", "Error al descargar imagen", e);
                                    });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
        binding.recyclerViewEgresos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewEgresos.setAdapter(adapter);

        configurarSwipe();

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
    private void configurarSwipe() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Egreso egreso = listaEgresos.get(position);

                new AlertDialog.Builder(EgresoActivity.this)
                        .setTitle("¿Eliminar egreso?")
                        .setMessage("¿Estás segura de que deseas eliminar \"" + egreso.getTitulo() + "\" y su comprobante?")
                        .setPositiveButton("Sí", (dialog, which) -> {

                            egresoRepository.eliminarEgreso(egreso.getId(),
                                    unused -> {
                                        Log.d("EgresoActivity", "Egreso eliminado de Firestore: " + egreso.getId());

                                        eliminarComprobanteStorage(egreso);

                                        listaEgresos.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        Toast.makeText(EgresoActivity.this, "Egreso y comprobante eliminados", Toast.LENGTH_SHORT).show();
                                    },
                                    e -> {
                                        Log.e("EgresoActivity", "Error al eliminar egreso", e);
                                        Toast.makeText(EgresoActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                paint.setColor(ContextCompat.getColor(EgresoActivity.this, android.R.color.holo_red_dark));
                RectF background = new RectF(itemView.getRight() + dX, itemView.getTop() + 8,
                        itemView.getRight() - 8, itemView.getBottom() - 8);
                c.drawRoundRect(background, 24, 24, paint);

                Drawable deleteIcon = ContextCompat.getDrawable(EgresoActivity.this, R.drawable.ic_delete);
                if (deleteIcon != null) {
                    int iconSize = 80;
                    int iconMargin = (itemView.getHeight() - iconSize) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconRight = itemView.getRight() - iconMargin - 16;

                    deleteIcon.setBounds(iconRight - iconSize, iconTop, iconRight, iconTop + iconSize);
                    deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    deleteIcon.draw(c);
                }

                float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth() * 0.3f;
                itemView.setAlpha(Math.max(alpha, 0.7f));

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewEgresos);
    }

    private void eliminarComprobanteStorage(Egreso egreso) {
        if (egreso.getComprobanteNombre() == null || egreso.getComprobanteNombre().isEmpty()) {
            Log.d("EgresoActivity", "El egreso no tiene comprobante para eliminar");
            return;
        }

        Log.d("EgresoActivity", "Eliminando comprobante: " + egreso.getComprobanteNombre());

        ServioAlmacenamiento servicioAlmacenamiento = new ServioAlmacenamiento();
        servicioAlmacenamiento.eliminarArchivo(
                egreso.getComprobanteNombre(),
                success -> {
                    Log.d("EgresoActivity", "Comprobante eliminado exitosamente: " + egreso.getComprobanteNombre());
                },
                error -> {
                    Log.e("EgresoActivity", "Error al eliminar comprobante: " + egreso.getComprobanteNombre(), error);
                }
        );
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

