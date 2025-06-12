package com.example.telemoney;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.ParseException;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.telemoney.Model.Egreso;
import com.example.telemoney.Repository.EgresoRepository;
import com.example.telemoney.databinding.ActivityAddEgresoBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEgresoActivity extends AppCompatActivity {
    ActivityAddEgresoBinding binding;
    private final static String TAG = "AddEgresoActivity";
    private EgresoRepository egresoRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEgresoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        egresoRepository = new EgresoRepository();
        configurarFormulario();
        configurarBotones();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(AddEgresoActivity.this, EgresoActivity.class);
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
                guardarNuevoEngreso();
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
        return true;
    }
    private void guardarNuevoEngreso() {
        String titulo = binding.editTextTitulo.getText().toString().trim();
        double monto = Double.parseDouble(binding.editTextMonto.getText().toString().trim());
        String descripcion = binding.editTextDescripcion.getText().toString().trim();
        String fecha = binding.editTextFecha.getText().toString().trim();

        Log.d(TAG, "Datos ingresados -> Titulo: " + titulo + ", Monto: " + monto + ", Desc: " + descripcion + ", Fecha: " + fecha);

        EgresoRepository repo = new EgresoRepository();

        // Paso 1: Generar nuevo ID
        repo.generarNuevoId(id -> {
            Log.d(TAG, "ID generado por el repositorio: " + id);

            Egreso nuevoEgreso = new Egreso(id, titulo, monto, descripcion, fecha);

            // Paso 2: Guardar en Firestore
            repo.guardarEgreso(nuevoEgreso,
                    unused -> {
                        Log.d(TAG, "Egreso guardado exitosamente");

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("nuevo_egreso", nuevoEgreso);
                        setResult(RESULT_OK, resultIntent);
                        Toast.makeText(this, "Egreso guardado exitosamente", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    e -> {
                        Log.e(TAG, "Error al guardar egreso", e);
                        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        }, e -> {
            Log.e(TAG, "Error al generar ID", e);
            Toast.makeText(this, "Error al generar ID", Toast.LENGTH_SHORT).show();
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
                        Intent intent = new Intent(AddEgresoActivity.this, EgresoRepository.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();

                    })
                    .setNegativeButton("Continuar editando", null)
                    .show();
        } else {
            Intent intent = new Intent(AddEgresoActivity.this, EgresoRepository.class);
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