package com.example.telemoney;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.ParseException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.telemoney.Model.Egreso;
import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.Repository.EgresoRepository;
import com.example.telemoney.Repository.IngresoRepository;
import com.example.telemoney.databinding.ActivityResumenBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResumenActivity extends AppCompatActivity {
    ActivityResumenBinding binding;
    private Calendar  mesSeleccionado;
    private IngresoRepository ingresoRepo;
    private EgresoRepository egresoRepo;
    private final static String TAG = "ResumenActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResumenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.bottomNavigation.setSelectedItemId(R.id.nav_resumen);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_resumen) {
                return true; // Ya estás en esta pantalla
            } else if (itemId == R.id.nav_ingresos) {
                startActivity(new Intent(this, IngresoActivity.class));
                overridePendingTransition(0, 0); // sin animación
                finish();
                return true;
            } else if (itemId == R.id.nav_egresos) {
                startActivity(new Intent(this, EgresoActivity.class));
                overridePendingTransition(0, 0); // sin animación
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

        ingresoRepo = new IngresoRepository();
        egresoRepo = new EgresoRepository();
        // Configurar DatePicker
        configurarDatePicker();
        // Inicializar mes actual
        mesSeleccionado = Calendar.getInstance();
        actualizarTextoMes();
        // Cargar datos al iniciar
        cargarDatosDelMes(mesSeleccionado);

    }

    private void configurarDatePicker() {
        binding.buttonMes.setOnClickListener(view -> mostrarSelectorMes());
    }
    private void actualizarTextoMes() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", new Locale("es"));
        String texto = formatter.format(mesSeleccionado.getTime());
        texto = texto.substring(0, 1).toUpperCase() + texto.substring(1);
        binding.buttonMes.setText(texto);
    }
    private void mostrarSelectorMes() {
        int añoActual = mesSeleccionado.get(Calendar.YEAR);
        int mesActual = mesSeleccionado.get(Calendar.MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    mesSeleccionado.set(Calendar.YEAR, year);
                    mesSeleccionado.set(Calendar.MONTH, month);
                    mesSeleccionado.set(Calendar.DAY_OF_MONTH, 1); // Primer día del mes

                    actualizarTextoMes();
                    Log.d(TAG, "Mes seleccionado: " + mesSeleccionado.getTime());
                    cargarDatosDelMes(mesSeleccionado);
                },
                añoActual,
                mesActual,
                1
        );

        ocultarSelectorDia(dialog);
        dialog.setTitle("Seleccionar mes");
        dialog.show();
    }
    private void ocultarSelectorDia(DatePickerDialog dialog) {
        try {
            View dayView = dialog.getDatePicker().findViewById(
                    getResources().getIdentifier("day", "id", "android")
            );
            if (dayView != null) {
                dayView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo ocultar el selector de día", e);
        }
    }
    private void cargarDatosDelMes(Calendar mes) {
        Log.d(TAG, "Cargando datos para el mes: " + mes.getTime());

        ingresoRepo.obtenerIngresos(listaIngresos -> {
            egresoRepo.obtenerEgresos(listaEgresos -> {
                procesarDatos(listaIngresos, listaEgresos, mes);
            }, e -> {
                Log.e(TAG, "Error cargando egresos", e);
                Toast.makeText(this, "Error al cargar egresos", Toast.LENGTH_LONG).show();
            });
        }, e -> {
            Log.e(TAG, "Error cargando ingresos", e);
            Toast.makeText(this, "Error al cargar ingresos", Toast.LENGTH_LONG).show();
        });
    }
    private void procesarDatos(List<Ingreso> ingresos, List<Egreso> egresos, Calendar mes) {
        // Filtrar por mes usando Calendar
        List<Ingreso> ingresosFiltrados = filtrarIngresosPorMes(ingresos, mes);
        List<Egreso> egresosFiltrados = filtrarEgresosPorMes(egresos, mes);

        // Resto del procesamiento igual...
        double totalMontoIngresos = calcularTotalIngresos(ingresosFiltrados);
        double totalMontoEgresos = calcularTotalEgresos(egresosFiltrados);
        double consolidado = totalMontoIngresos + totalMontoEgresos;

        float porcentajeIngresos = 0f;
        float porcentajeEgresos = 0f;

        if (totalMontoIngresos > 0) {
            porcentajeIngresos = (float) ((totalMontoIngresos * 100.0) / (totalMontoIngresos + totalMontoEgresos));
            porcentajeEgresos = (float) ((totalMontoEgresos * 100.0) / (totalMontoIngresos + totalMontoEgresos));
        }

        actualizarPieChart(porcentajeIngresos, porcentajeEgresos, totalMontoIngresos, totalMontoEgresos);
        actualizarBarChart(totalMontoIngresos, totalMontoEgresos, consolidado);

        Log.d(TAG, "Datos procesados - Ingresos: $" + totalMontoIngresos + " (" + porcentajeIngresos + "%), " +
                "Egresos: $" + totalMontoEgresos + " (" + porcentajeEgresos + "%)");
    }
    private List<Ingreso> filtrarIngresosPorMes(List<Ingreso> ingresos, Calendar mes) {
        List<Ingreso> ingresosFiltrados = new ArrayList<>();

        for (Ingreso ingreso : ingresos) {
            if (perteneceAlMes(ingreso.getFecha(), mes)) {
                ingresosFiltrados.add(ingreso);
            }
        }

        Log.d(TAG, "Ingresos filtrados: " + ingresosFiltrados.size() + " de " + ingresos.size());
        return ingresosFiltrados;
    }

    private List<Egreso> filtrarEgresosPorMes(List<Egreso> egresos, Calendar mes) {
        List<Egreso> egresosFiltrados = new ArrayList<>();

        for (Egreso egreso : egresos) {
            if (perteneceAlMes(egreso.getFecha(), mes)) {
                egresosFiltrados.add(egreso);
            }
        }

        Log.d(TAG, "Egresos filtrados: " + egresosFiltrados.size() + " de " + egresos.size());
        return egresosFiltrados;
    }
    private boolean perteneceAlMes(String fecha, Calendar mes) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(fecha);

            if (date != null) {
                Calendar fechaCalendar = Calendar.getInstance();
                fechaCalendar.setTime(date);

                int añoFecha = fechaCalendar.get(Calendar.YEAR);
                int mesFecha = fechaCalendar.get(Calendar.MONTH);

                int añoSeleccionado = mes.get(Calendar.YEAR);
                int mesSeleccionado = mes.get(Calendar.MONTH);

                return añoFecha == añoSeleccionado && mesFecha == mesSeleccionado;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parseando fecha: " + fecha, e);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    private void actualizarPieChart(float porcentajeIngresos, float porcentajeEgresos, double montoIngresos, double montoEgresos) {
        PieChart pieChart = binding.pieChart;
        List<PieEntry> entries = new ArrayList<>();
        if (porcentajeIngresos > 0) {
            entries.add(new PieEntry(porcentajeIngresos, "Ingresos ($" + String.format(Locale.getDefault(), "%.0f", montoIngresos) + ")"));
        }
        if (porcentajeEgresos > 0) {
            entries.add(new PieEntry(porcentajeEgresos, "Egresos ($" + String.format(Locale.getDefault(), "%.0f", montoEgresos) + ")"));
        }
        if (entries.isEmpty()) {
            mostrarPieChartSinDatos();
            return;
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        configurarEstiloPieChart(dataSet, porcentajeIngresos, porcentajeEgresos);
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        configurarPieChart(pieChart);
        pieChart.invalidate();
    }
    private void configurarEstiloPieChart(PieDataSet dataSet, float porcentajeIngresos, float porcentajeEgresos) {
        List<Integer> colors = new ArrayList<>();
        if (porcentajeIngresos > 0) colors.add(ContextCompat.getColor(this, R.color.green_500));
        if (porcentajeEgresos > 0) colors.add(ContextCompat.getColor(this, R.color.red_500));

        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });
    }
    private void configurarPieChart(PieChart pieChart) {
        pieChart.setUsePercentValues(false);
        pieChart.setDescription(null);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(35f);
        pieChart.setCenterText("Distribución\nPorcentual");
        pieChart.setCenterTextSize(14f);
        pieChart.setEntryLabelTextSize(12f);

        Legend legend = pieChart.getLegend();
        legend.setTextSize(12f);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
    }
    private void mostrarPieChartSinDatos() {
        PieChart pieChart = binding.pieChart;
        pieChart.clear();
        pieChart.setCenterText("Sin datos\npara este mes");
        pieChart.setCenterTextSize(16f);
        pieChart.invalidate();
    }
    private void actualizarBarChart(double ingresos, double egresos, double consolidado) {
        BarChart barChart = binding.barChart;

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) ingresos));
        entries.add(new BarEntry(1f, (float) egresos));
        entries.add(new BarEntry(2f, (float) consolidado));

        BarDataSet dataSet = new BarDataSet(entries, "");

        List<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(this, R.color.green_500));
        colors.add(ContextCompat.getColor(this, R.color.red_500));
        colors.add(ContextCompat.getColor(this, R.color.blue_500));

        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "$" + String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private String[] labels = {"Ingresos", "Egresos", "Total"};
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < labels.length ? labels[index] : "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "$" + String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        barChart.getAxisRight().setEnabled(false);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);

        barChart.invalidate();

        Log.d(TAG, "BarChart actualizado - Ingresos: $" + ingresos +
                ", Egresos: $" + egresos + ", Consolidado: $" + consolidado);
    }
    private double calcularTotalIngresos(List<Ingreso> ingresos) {
        return ingresos.stream().mapToDouble(Ingreso::getMonto).sum();
    }
    private double calcularTotalEgresos(List<Egreso> egresos) {
        return egresos.stream().mapToDouble(Egreso::getMonto).sum();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}