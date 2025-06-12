package com.example.telemoney.Adpater;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemoney.Model.Ingreso;
import com.example.telemoney.R;

import java.util.List;

public class IngresoAdapter extends RecyclerView.Adapter<IngresoAdapter.IngresoViewHolder> {

    private List<Ingreso> lista;
    private final OnIngresoClickListener listener;

    public interface OnIngresoClickListener {
        void onEdit(Ingreso ingreso);
        void onDelete(Ingreso ingreso);
    }

    public IngresoAdapter(List<Ingreso> lista, OnIngresoClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IngresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingreso, parent, false);
        return new IngresoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull IngresoViewHolder holder, int position) {
        Ingreso ingreso = lista.get(position);
        holder.bind(ingreso);
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public class IngresoViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, monto, descripcion, fecha;

        public IngresoViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.textViewTitulo);
            monto = itemView.findViewById(R.id.textViewMonto);
            descripcion = itemView.findViewById(R.id.textViewDescripcion);
            fecha = itemView.findViewById(R.id.textViewFecha);
        }

        public void bind(Ingreso ingreso) {
            titulo.setText(ingreso.getTitulo());
            monto.setText(String.format("S/ %.2f", ingreso.getMonto()));
            descripcion.setText(ingreso.getDescripcion());
            fecha.setText(ingreso.getFecha());

            itemView.setOnLongClickListener(view -> {
                listener.onDelete(ingreso); // Llama al listener para eliminar el ingreso
                return true;
            });

            itemView.setOnClickListener(view -> {
                listener.onEdit(ingreso); // Llama al listener para editar el ingreso
            });
        }
    }
}

