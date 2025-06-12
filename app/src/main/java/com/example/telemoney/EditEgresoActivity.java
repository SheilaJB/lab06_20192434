package com.example.telemoney;

import android.content.Intent;
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
import com.example.telemoney.databinding.ActivityEditEgresoBinding;

public class EditEgresoActivity extends AppCompatActivity {
    ActivityEditEgresoBinding binding;
    private final static String TAG = "EditEgresoActivity";
    private Egreso egresoOriginal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditEgresoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Recibir el egreso original
        recibirEgresoOriginal();
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
    private void recibirEgresoOriginal() {
        if (getIntent() != null && getIntent().hasExtra("egreso")) {
            egresoOriginal = (Egreso) getIntent().getSerializableExtra("egreso");
            if (egresoOriginal != null) {
                // Aquí puedes usar los datos del egresoOriginal para llenar los campos de edición
                binding.editTextTitulo.setText(egresoOriginal.getTitulo());
                binding.editTextMonto.setText(String.valueOf(egresoOriginal.getMonto()));
                binding.editTextDescripcion.setText(egresoOriginal.getDescripcion());
                binding.editTextFecha.setText(egresoOriginal.getFecha());
                Log.d(TAG, "Egreso recibido: " + egresoOriginal.getTitulo());

            }
        } else {
            finish();
        }
    }

    private void configurarCamposEditables() {
        // Aquí puedes configurar los campos editables si es necesario
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

        // Actualizar los datos en el objeto original
        egresoOriginal.setMonto(monto);
        egresoOriginal.setDescripcion(descripcion);

        EgresoRepository repository = new EgresoRepository();

        repository.actualizarEgreso(egresoOriginal,
                unused -> {
                    Log.d(TAG, "Egreso actualizado en Firestore: " + egresoOriginal.getTitulo());

                    Intent intent = new Intent();
                    intent.putExtra("egreso_actualizado", egresoOriginal);
                    setResult(RESULT_OK, intent);
                    finish();
                },
                e -> {
                    Log.e(TAG, "Error al actualizar egreso", e);
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
        if (egresoOriginal == null) return false;

        try {
            double montoActual = Double.parseDouble(binding.editTextMonto.getText().toString().trim());
            String descripcionActual = binding.editTextDescripcion.getText().toString().trim();

            return montoActual != egresoOriginal.getMonto() ||
                    !descripcionActual.equals(egresoOriginal.getDescripcion());
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