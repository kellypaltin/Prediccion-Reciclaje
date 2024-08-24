package com.example.projectap;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class SecondActivity extends AppCompatActivity {
    private static final String TAG = "Model";
    private Interpreter tflite;
    private static final String[] CLASS_NAMES = {"Cartón", "Vidrio", "Metal", "Papel", "Plástico", "Basura"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Button camera;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        try {
            tflite = new Interpreter(loadModelFile());
            int[] inputShape = tflite.getInputTensor(0).shape();
            int[] outputShape = tflite.getOutputTensor(0).shape();
            int inputChannels = inputShape[3];

            Log.d("Model Info", "Input Shape: " + Arrays.toString(inputShape));
            Log.d("Model Info", "Output Shape: " + Arrays.toString(outputShape));
            Log.d("Model Info", "Input Channels: " + inputChannels);
            Log.d(TAG, "TensorFlow Lite model loaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "TensorFlow Lite model loaded error.");

        }

        camera=(Button)findViewById(R.id.button2);
        camera.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, Camara.class);
            startActivity(intent);
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        Resources resources = getResources();
        int modelResourceId = R.raw.resnet50_garbage_classification;
        FileInputStream inputStream = new FileInputStream(resources.openRawResourceFd(modelResourceId).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = resources.openRawResourceFd(modelResourceId).getStartOffset();
        long declaredLength = resources.openRawResourceFd(modelResourceId).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float[][] runInference(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        int batchSize = 1;
        int inputSize = 224;
        int numChannels = 3;
        float[][][][] input = new float[batchSize][numChannels][inputSize][inputSize];

        for (int x = 0; x < inputSize; x++) {
            for (int y = 0; y < inputSize; y++) {
                int pixel = resizedBitmap.getPixel(x, y);
                input[0][0][x][y] = ((pixel >> 16) & 0xFF) / 255.0f; // Rojo
                input[0][1][x][y] = ((pixel >> 8) & 0xFF) / 255.0f;  // Verde
                input[0][2][x][y] = (pixel & 0xFF) / 255.0f;         // Azul
            }
        }

        float[][] output = new float[1][6];  // Ajusta el tamaño de salida según el modelo

        tflite.run(input, output);

        return output;
    }

    private String clasePredecir(float[][] result){
        int maxIndex = 0;
        float maxProb = result[0][0];
        for (int i = 1; i < 6; i++) {
            if (result[0][i] > maxProb) {
                maxProb = result[0][i];
                maxIndex = i;
            }
        }
        String predictedClass = CLASS_NAMES[maxIndex];
        return predictedClass;
    }

    private int imgPre(String prediccion){
        String predic = prediccion.toLowerCase();
        int imageResource;
        if (predic.contains("papel") || predic.contains("cartón")) {
            imageResource = R.drawable.tachoazul;
            return imageResource;
        } else if (predic.contains("plástico")) {
            imageResource = R.drawable.tachon;
            return imageResource;
        } else if (predic.contains("basura")) {
            imageResource = R.drawable.tachogris;
            return imageResource;
        } else if (predic.contains("metal")) {
            imageResource = R.drawable.tachoam;
            return imageResource;
        } else if (predic.contains("vidrio")) {
            imageResource = R.drawable.tachove;
            return imageResource;
        } else if (predic.contains("peligroso")) {
            imageResource = R.drawable.tachor;
            return imageResource;
        } else {
            imageResource = R.drawable.tachor;
            return imageResource;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("imagePath");
        if (imagePath != null) {
            ImageView imageView = findViewById(R.id.image_view);
            TextView textResult = findViewById(R.id.textView4);
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            imageView.setImageBitmap(bitmap);
            float[][] result = runInference(bitmap);
            String predic = clasePredecir(result);
            ImageView imga = findViewById(R.id.image_viewpre);
            int bit = imgPre(predic);
            imga.setImageResource(bit);
            textResult.append(predic);
            Log.d(TAG, Arrays.deepToString(result));
        }
    }
}