package com.example.telemoney.Adpater;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemoney.Model.Egreso;
import com.example.telemoney.R;

import java.util.List;

public class EgresoAdapter extends  RecyclerView.Adapter<EgresoAdapter.EgresoViewHolder>{

    private List<Egreso> lista;
    private final OnEgresoClickListener listener;
    public interface OnEgresoClickListener {
        void onEdit(Egreso egreso);
        void onDelete(Egreso egreso);
    }
    public EgresoAdapter(List<Egreso> lista, OnEgresoClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EgresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_egreso, parent, false);
        return new EgresoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull EgresoViewHolder holder, int position) {
        Egreso egreso = lista.get(position);
        holder.bind(egreso);
    }
    @Override
    public int getItemCount() {
        return lista.size();
    }
    public class EgresoViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, monto, descripcion, fecha;

        public EgresoViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.textViewTitulo);
            monto = itemView.findViewById(R.id.textViewMonto);
            descripcion = itemView.findViewById(R.id.textViewDescripcion);
            fecha = itemView.findViewById(R.id.textViewFecha);
        }

        public void bind(Egreso egreso) {
            titulo.setText(egreso.getTitulo());
            monto.setText(String.format("S/ %.2f", egreso.getMonto()));
            descripcion.setText(egreso.getDescripcion());
            fecha.setText(egreso.getFecha());

            itemView.setOnLongClickListener(view -> {
                listener.onDelete(egreso); // Llama al listener para eliminar el ingreso
                return true;
            });

            itemView.setOnClickListener(view -> {
                listener.onEdit(egreso); // Llama al listener para editar el ingreso
            });
        }
    }
}
