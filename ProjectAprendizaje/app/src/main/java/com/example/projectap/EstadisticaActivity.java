package com.example.projectap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class EstadisticaActivity extends AppCompatActivity {

    private static final String TAG = "Estadistica";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Map<String, Integer> predictionCounts = new HashMap<>();
    private String[] predictionTypes = {"Batería", "Biológico", "Vidrio", "Carton", "Ropa", "Metal", "Papel", "Plástico", "Zapatos", "Basura"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estadistica);

        BarChart barChart = findViewById(R.id.barChart);
        PieChart pieChart = findViewById(R.id.pieChart);

        for (String type : predictionTypes) {
            predictionCounts.put(type, 0);
        }

        // Obtén los datos de Firestore
        db.collection("Predicciones")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String prediction = document.getString("name");
                                if (prediction != null && predictionCounts.containsKey(prediction)) {
                                    predictionCounts.put(prediction, predictionCounts.get(prediction) + 1);
                                }
                            }

                            // Prepara los datos para el gráfico de barras
                            List<BarEntry> barEntries = new ArrayList<>();
                            List<PieEntry> pieEntries = new ArrayList<>();
                            int index = 0;
                            for (String type : predictionTypes) {
                                int count = predictionCounts.get(type);
                                barEntries.add(new BarEntry(index, count));
                                pieEntries.add(new PieEntry(count, type));
                                index++;
                            }

                            // Configurar el gráfico de barras
                            BarDataSet barDataSet = new BarDataSet(barEntries, "Predicciones");
                            BarData barData = new BarData(barDataSet);
                            barChart.setData(barData);
                            barChart.invalidate(); // Actualiza el gráfico

                            // Configuración adicional del gráfico de barras
                            XAxis xAxis = barChart.getXAxis();
                            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                            xAxis.setGranularity(1f);
                            xAxis.setLabelRotationAngle(-45);
                            xAxis.setValueFormatter(new IndexAxisValueFormatter(predictionTypes));

                            YAxis leftAxis = barChart.getAxisLeft();
                            leftAxis.setDrawGridLines(false);

                            YAxis rightAxis = barChart.getAxisRight();
                            rightAxis.setDrawGridLines(false);
                            rightAxis.setEnabled(false);


                            // Configurar el gráfico de pastel
                            PieDataSet pieDataSet = new PieDataSet(pieEntries, "Predicciones");
                            pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                            pieDataSet.setDrawValues(false); // Ocultar los valores en el gráfico
                            PieData pieData = new PieData(pieDataSet);
                            pieChart.setData(pieData);
                            pieChart.invalidate();

                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }
}