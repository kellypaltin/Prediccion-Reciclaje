package com.example.projectap;


import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import org.tensorflow.lite.Interpreter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import firebase.com.protolitewrapper.BuildConfig;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.FileOutputStream;
import java.util.Map;

import retrofit2.Callback;
import retrofit2.Response;

public class SecondActivity extends AppCompatActivity {
    private static final String TAG = "Model";
    private Interpreter tflite;
    private static final String BASE_URL = "https://sdk.photoroom.com/v1/";
    private static final String API_KEY = "5d5b51b47c9617eb41b5f0013bb69c64f29856731";
    private String textPre;
    TextToSpeech t1;
    private static final String[] CLASS_NAMES = {"Batería", "Biológico", "Vidrio", "Carton","Ropa","Vidrio","Metal","Papel", "Plástico","Zapatos", "Basura","Vidrio"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Button camera,analisis;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        FirebaseApp.initializeApp(this);

        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    t1.setLanguage(Locale.ENGLISH);
                }
            }
        });

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

        analisis=(Button)findViewById(R.id.button3);
        analisis.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, EstadisticaActivity.class);
            startActivity(intent);
        });
    }

    public interface PhotoRoomService {
        @Headers({
                "Accept: image/png, application/json",
                "x-api-key: 5d5b51b47c9617eb41b5f0013bb69c64f2985673"
        })
        @Multipart
        @POST("segment")
        Call<ResponseBody> removeBg(@Part MultipartBody.Part file);
    }

   //Despliega el modelo cargado 
   //Ruta: src/main/res/raw
    private MappedByteBuffer loadModelFile() throws IOException {
        Resources resources = getResources();
        int modelResourceId = R.raw.resnet50_classification;
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
                input[0][0][x][y] = ((pixel >> 16) & 0xFF) / 255.0f;
                input[0][1][x][y] = ((pixel >> 8) & 0xFF) / 255.0f;
                input[0][2][x][y] = (pixel & 0xFF) / 255.0f;
            }
        }

        float[][] output = new float[1][12];

        tflite.run(input, output);

        return output;
    }


    public static class RetrofitClient {
        public static Retrofit retrofit = null;

        public static Retrofit getClient(String baseUrl) {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
            return retrofit;
        }
    }

    private void removeBackground(String inputImagePath, String outputImagePath) {
        File inputFile = new File(inputImagePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("application/octet-stream"), inputFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image_file", inputFile.getName(), requestFile);

        PhotoRoomService service = RetrofitClient.getClient(BASE_URL).create(PhotoRoomService.class);
        Call<ResponseBody> call = service.removeBg(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] responseBodyBytes = response.body().bytes();
                        File outputFile = new File(outputImagePath);
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            fos.write(responseBodyBytes);
                            Log.d("API_CALL", "Image downloaded and saved to " + outputImagePath);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("API_CALL", "Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API_CALL", "Error: " + t.getMessage());
            }
        });
    }

    private String clasePredecir(float[][] result){
        int maxIndex = 0;
        float maxProb = result[0][0];
        for (int i = 1; i < 12; i++) {
            if (result[0][i] > maxProb) {
                maxProb = result[0][i];
                maxIndex = i;
            }
        }
        String predictedClass = CLASS_NAMES[maxIndex];
        return predictedClass;
    }

    private int imgPre(String prediccion) {
        String predic = prediccion.toLowerCase();
        int imageResource;
        if (predic.contains("papel") || predic.contains("cartón")) {
            imageResource = R.drawable.tachoazul;
            textPre = "La prediccion es "+predic+" y va en el bote de basura azul";
            return imageResource;
        } else if (predic.contains("plástico")) {
            imageResource = R.drawable.tachon;
            textPre = "La prediccion es "+predic+" y va en el bote de basura naranja";
            return imageResource;
        } else if (predic.contains("basura")) {
            imageResource = R.drawable.tachogris;
            textPre = "La prediccion es "+predic+" y va en el bote de basura gris";
            return imageResource;
        } else if (predic.contains("metal")) {
            imageResource = R.drawable.tachoam;
            textPre = "La prediccion es "+predic+" y va en el bote de basura amarillo";
            return imageResource;
        } else if (predic.contains("vidrio")) {
            imageResource = R.drawable.tachove;
            textPre = "La prediccion es "+predic+" y va en el bote de basura verde";
            return imageResource;
        } else if (predic.contains("peligroso")) {
            imageResource = R.drawable.tachor;
            textPre = "La prediccion es "+predic+" y va en el bote de basura rojo";
            return imageResource;
        } else {
            imageResource = R.drawable.tachor;
            return imageResource;
        }
    }

    public Uri getImageUriFromPath(String imagePath) {
        File file = new File(imagePath);
        return Uri.fromFile(file);
    }


    private void uploadImage(Uri imageUri, final String name) {
        if (imageUri != null) {
            // Obteniendo la referencia de Firebase Storage
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference().child("images/" + System.currentTimeMillis() + ".jpg");


            storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            String imageUrl = uri.toString();

                            saveDataToDatabase(name, imageUrl);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Toast.makeText(SecondActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDataToDatabase(String name, String imageUrl) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();


        CollectionReference predictionsRef = db.collection("Predicciones");
        String documentId = predictionsRef.document().getId();


        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("imageUrl", imageUrl);


        predictionsRef.document(documentId).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(SecondActivity.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    Log.e("DataSource", "Data saved successfully: ");
                } else {
                    Toast.makeText(SecondActivity.this, "Failed to save data", Toast.LENGTH_SHORT).show();
                    Log.e("DataSource", "Data failed to save: ", task.getException());
                }
            }
        });
    }


    public class User {
        public String prediction;
        public String imageUrl;

        public User() {
        }

        public User(String prediction, String imageUrl) {
            this.prediction = prediction;
            this.imageUrl = imageUrl;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        File outputImagePath = new File(getExternalFilesDir(null), "/output_image.png");

        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("imagePath");

        if (imagePath != null) {
            Log.d(TAG, "La se guardara en  "+outputImagePath.getAbsolutePath());
            Log.d(TAG, "La imagen es "+imagePath);

	    removeBackground(imagePath,outputImagePath)
            ImageView imageView = findViewById(R.id.image_view);
            TextView textResult = findViewById(R.id.textView4);
            Bitmap bitmap = BitmapFactory.decodeFile(outputImagePath);

            imageView.setImageBitmap(bitmap);
            float[][] result = runInference(bitmap);
            String predic = clasePredecir(result);
            ImageView imga = findViewById(R.id.image_viewpre);
            int bit = imgPre(predic);
            imga.setImageResource(bit);
            textResult.append(predic);
            Uri imageUri = getImageUriFromPath(imagePath);
            Log.d(TAG, "El uri es este  "+imageUri);

            uploadImage(imageUri,predic);
            t1.speak(textPre,TextToSpeech.QUEUE_FLUSH, null);
            Log.d(TAG, Arrays.deepToString(result));
        }
    }
}
