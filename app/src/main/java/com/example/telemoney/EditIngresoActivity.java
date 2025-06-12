package com.example.telemoney;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.Repository.IngresoRepository;
import com.example.telemoney.databinding.ActivityEditIngresoBinding;

public class EditIngresoActivity extends AppCompatActivity {
    ActivityEditIngresoBinding binding;
    private Ingreso ingresoOriginal;
    private final static String TAG = "EditIngresoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditIngresoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Recibir el ingreso original
        recibirIngresoOriginal();
        configurarCamposEditables();
        configurarBotones();
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void recibirIngresoOriginal(){
        if (getIntent() != null && getIntent().hasExtra("ingreso")) {
            ingresoOriginal = (Ingreso) getIntent().getSerializableExtra("ingreso");
            if (ingresoOriginal != null) {
                // Aquí puedes usar los datos del ingresoOriginal para llenar los campos de edición
                binding.editTextTitulo.setText(ingresoOriginal.getTitulo());
                binding.editTextMonto.setText(String.valueOf(ingresoOriginal.getMonto()));
                binding.editTextDescripcion.setText(ingresoOriginal.getDescripcion());
                binding.editTextFecha.setText(ingresoOriginal.getFecha());
                Log.d(TAG, "Ingreso recibido: " + ingresoOriginal.getTitulo());
            }
        }else {
            Log.e(TAG, "No se recibió el objeto Ingreso");
            Toast.makeText(this, "Error: No se pudieron cargar los datos", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void configurarCamposEditables() {
        binding.editTextTitulo.setEnabled(false);
        binding.editTextFecha.setEnabled(false);

        binding.editTextTitulo.setTextColor(getResources().getColor(android.R.color.darker_gray));
        binding.editTextFecha.setTextColor(getResources().getColor(android.R.color.darker_gray));

        binding.editTextTitulo.setHint("Título (No editable)");
        binding.editTextFecha.setHint("Fecha (No editable)");
    }

    private void configurarBotones() {
        binding.buttonGuardar.setOnClickListener(view -> {
            if (validarCampos()) {
                guardarCambios();
            }
        });
        binding.buttonCancel.setOnClickListener(view -> {
            mostrarConfirmacionCancelacion();
        });
    }
    private boolean validarCampos() {
        String montoStr = binding.editTextMonto.getText().toString().trim();
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
        return true;
    }

    private void guardarCambios() {
        double monto = Double.parseDouble(binding.editTextMonto.getText().toString());
        String descripcion = binding.editTextDescripcion.getText().toString();

        ingresoOriginal.setMonto(monto);
        ingresoOriginal.setDescripcion(descripcion);
        IngresoRepository repo = new IngresoRepository();
        repo.actualizarIngreso(ingresoOriginal,
                unused -> {
                    Log.d(TAG, "Ingreso actualizado en Firestore");

                    Intent intent = new Intent();
                    intent.putExtra("ingreso_actualizado", ingresoOriginal);
                    setResult(RESULT_OK, intent);
                    finish();
                },
                e -> {
                    Log.e(TAG, "Error al actualizar ingreso en Firestore", e);
                    Toast.makeText(this, "Error al guardar cambios", Toast.LENGTH_SHORT).show();
                });
    }
    private void mostrarConfirmacionCancelacion() {
        if (hayCambiosSinGuardar()) {
            new AlertDialog.Builder(this)
                    .setTitle("¿Cancelar edición?")
                    .setMessage("Se perderán los cambios realizados")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .setNegativeButton("Continuar editando", null)
                    .show();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
    private boolean hayCambiosSinGuardar() {
        if (ingresoOriginal == null) return false;

        try {
            double montoActual = Double.parseDouble(binding.editTextMonto.getText().toString().trim());
            String descripcionActual = binding.editTextDescripcion.getText().toString().trim();

            return montoActual != ingresoOriginal.getMonto() ||
                    !descripcionActual.equals(ingresoOriginal.getDescripcion());
        } catch (NumberFormatException e) {
            return true; // Si hay error en el parsing, considerar que hay cambios
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}