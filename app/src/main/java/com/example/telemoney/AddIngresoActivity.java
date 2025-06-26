package com.example.telemoney;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.Repository.IngresoRepository;
import com.example.telemoney.databinding.ActivityAddIngresoBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddIngresoActivity extends AppCompatActivity {
    private IngresoRepository ingresoRepository;
    private ServioAlmacenamiento servicioAlmacenamiento;
    ActivityAddIngresoBinding binding;
    private final static String TAG = "AddIngresoActivity";
    private Uri imagenSeleccionadaUri;
    private MaterialCardView cardImagenPreview;
    private ImageView imageViewPreview;
    private FloatingActionButton fabRemoveImage;
    private TextView textViewImagenNombre;
    private LinearLayout layoutUploadStatus;
    private TextView textViewUploadStatus;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddIngresoBinding.inflate(getLayoutInflater());
        servicioAlmacenamiento = new ServioAlmacenamiento();
        servicioAlmacenamiento.conectarServicio();
        setContentView(binding.getRoot());
        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ingresoRepository = new IngresoRepository();
        configurarFormulario();
        configurarBotones();
        inicializarVistaImagen();
    }

    private void inicializarVistaImagen() {
        cardImagenPreview = binding.cardImagenPreview;
        imageViewPreview = binding.imageViewPreview;
        fabRemoveImage = binding.fabRemoveImage;
        textViewImagenNombre = binding.textViewImagenNombre;
        layoutUploadStatus = binding.layoutUploadStatus;
        textViewUploadStatus = binding.textViewUploadStatus;

        // Configurar visibilidad inicial
        binding.buttonSeleccionarImagen.setOnClickListener(v -> abrirSelectorImagen());

        // Configurar botón de eliminar imagen
        fabRemoveImage.setOnClickListener(v -> {
            imagenSeleccionadaUri = null;
            cardImagenPreview.setVisibility(View.GONE);
            binding.buttonSeleccionarImagen.setText("Seleccionar Comprobante");
        });
    }
    private void abrirSelectorImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imagenSeleccionadaUri = data.getData();
            if (imagenSeleccionadaUri != null) {
                mostrarImagenSeleccionada();
            }
        }
    }

    private void mostrarImagenSeleccionada() {
        imageViewPreview.setImageURI(imagenSeleccionadaUri);
        textViewImagenNombre.setText(getFileName(imagenSeleccionadaUri));
        cardImagenPreview.setVisibility(View.VISIBLE);
        binding.buttonSeleccionarImagen.setText("📸 Cambiar Comprobante");
    }

    private String getFileName(Uri uri) {
        String result = "comprobante.jpg";
        if (uri != null && "content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al obtener nombre del archivo", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(AddIngresoActivity.this, IngresoActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void configurarFormulario(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.editTextFecha.setText(sdf.format(new Date()));
        binding.editTextFecha.setOnClickListener(view -> mostrarSelectorFecha());
        binding.editTextTitulo.requestFocus();
    }
    private void configurarBotones() {
        binding.buttonGuardar.setOnClickListener(view -> {
            if (validarCampos()) {
                guardarNuevoIngreso();
            }
        });

        binding.buttonCancel.setOnClickListener(view -> {
            mostrarConfirmacionSalida();
        });
    }
    private boolean validarCampos() {
        String titulo = binding.editTextTitulo.getText().toString().trim();
        String montoStr = binding.editTextMonto.getText().toString().trim();
        String fecha = binding.editTextFecha.getText().toString().trim();

        // Validar título
        if (titulo.isEmpty()) {
            binding.editTextTitulo.setError("El título es requerido");
            binding.editTextTitulo.requestFocus();
            return false;
        }

        // Validar monto
        if (montoStr.isEmpty()) {
            binding.editTextMonto.setError("El monto es requerido");
            binding.editTextMonto.requestFocus();
            return false;
        }

        try {
            double monto = Double.parseDouble(montoStr);
            if (monto <= 0) {
                binding.editTextMonto.setError("El monto debe ser mayor a 0");
                binding.editTextMonto.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            binding.editTextMonto.setError("Ingrese un monto válido");
            binding.editTextMonto.requestFocus();
            return false;
        }

        // Validar fecha
        if (fecha.isEmpty()) {
            binding.editTextFecha.setError("La fecha es requerida");
            binding.editTextFecha.requestFocus();
            return false;
        }
        // Validar imagen seleccionada
        if (imagenSeleccionadaUri == null) {
            Toast.makeText(this, "El comprobante es obligatorio", Toast.LENGTH_LONG).show();
            binding.buttonSeleccionarImagen.requestFocus();
            return false;
        }

        return true;
    }
    private void guardarNuevoIngreso() {
        Log.d(TAG, "Inicio de guardarNuevoIngreso()");

        String titulo = binding.editTextTitulo.getText().toString().trim();
        double monto = Double.parseDouble(binding.editTextMonto.getText().toString().trim());
        String descripcion = binding.editTextDescripcion.getText().toString().trim();
        String fecha = binding.editTextFecha.getText().toString().trim();

        layoutUploadStatus.setVisibility(View.VISIBLE);
        textViewUploadStatus.setText("Subiendo comprobante...");
        binding.buttonGuardar.setEnabled(false);

        ingresoRepository.generarNuevoId(id -> {
            Log.d(TAG, "ID generado por el repositorio: " + id);

            String nombreArchivo = "comprobante_" + id + "_" + System.currentTimeMillis() + ".jpg";
            Log.d(TAG, "Nombre del archivo generado: " + nombreArchivo);
            servicioAlmacenamiento.guardarArchivo(imagenSeleccionadaUri, nombreArchivo, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Archivo subido exitosamente: " + nombreArchivo);
                    textViewUploadStatus.setText("Obteniendo enlace...");

                    servicioAlmacenamiento.obtenerArchivo(nombreArchivo, uri -> {
                        String urlComprobante = uri.toString();
                        Log.d(TAG, "URL del comprobante obtenida: " + urlComprobante);

                        textViewUploadStatus.setText("Guardando ingreso...");

                        Ingreso nuevoIngreso = new Ingreso(id, titulo, monto, descripcion, fecha);
                        nuevoIngreso.setComprobanteUrl(urlComprobante);
                        nuevoIngreso.setComprobanteNombre(nombreArchivo);

                        Log.d(TAG, "Guardando ingreso con comprobante - URL: " + urlComprobante + ", Nombre: " + nombreArchivo);
                        ingresoRepository.guardarIngreso(nuevoIngreso,
                                unused -> {
                                    Log.d(TAG, "Ingreso guardado exitosamente en Firestore");
                                    layoutUploadStatus.setVisibility(View.GONE);
                                    binding.buttonGuardar.setEnabled(true);
                                    Toast.makeText(this, "✅ Ingreso guardado exitosamente", Toast.LENGTH_SHORT).show();
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("nuevo_ingreso", nuevoIngreso);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                },
                                e -> {
                                    Log.e(TAG, "Error al guardar ingreso en Firestore", e);
                                    layoutUploadStatus.setVisibility(View.GONE);
                                    binding.buttonGuardar.setEnabled(true);
                                    Toast.makeText(this, "Error al guardar ingreso: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    }, e -> {
                        Log.e(TAG, "Error al obtener URL de descarga", e);
                        layoutUploadStatus.setVisibility(View.GONE);
                        binding.buttonGuardar.setEnabled(true);
                        Toast.makeText(this, "Error al obtener URL de comprobante: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

                } else {
                    Log.e(TAG, "Error al subir imagen", task.getException());
                    layoutUploadStatus.setVisibility(View.GONE);
                    binding.buttonGuardar.setEnabled(true);
                    Toast.makeText(this, "Error al subir comprobante: " + (task.getException() != null ? task.getException().getMessage() : "Error desconocido"), Toast.LENGTH_LONG).show();
                }
            });
        }, e -> {
            Log.e(TAG, "Error al generar ID", e);
            layoutUploadStatus.setVisibility(View.GONE);
            binding.buttonGuardar.setEnabled(true);
            Toast.makeText(this, "Error al generar ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private void mostrarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();

        // Si ya hay una fecha seleccionada, usarla como fecha inicial
        String fechaActual = binding.editTextFecha.getText().toString();
        if (!fechaActual.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date fecha = sdf.parse(fechaActual);
                if (fecha != null) {
                    calendar.setTime(fecha);
                }
            } catch (ParseException e) {
                // Si hay error, usar fecha actual
                calendar = Calendar.getInstance();
            } catch (java.text.ParseException e) {
                throw new RuntimeException(e);
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    binding.editTextFecha.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void mostrarConfirmacionSalida() {
        // Verificar si hay datos ingresados
        if (hayDatosIngresados()) {
            new AlertDialog.Builder(this)
                    .setTitle("¿Salir sin guardar?")
                    .setMessage("Se perderán los datos ingresados")
                    .setPositiveButton("Sí, salir", (dialog, which) -> {
                        Intent intent = new Intent(AddIngresoActivity.this, IngresoActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();

                    })
                    .setNegativeButton("Continuar editando", null)
                    .show();
        } else {
            Intent intent = new Intent(AddIngresoActivity.this, IngresoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();

        }
    }
    private boolean hayDatosIngresados() {
        String titulo = binding.editTextTitulo.getText().toString().trim();
        String monto = binding.editTextMonto.getText().toString().trim();
        String descripcion = binding.editTextDescripcion.getText().toString().trim();
        return !titulo.isEmpty() || !monto.isEmpty() || !descripcion.isEmpty();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}