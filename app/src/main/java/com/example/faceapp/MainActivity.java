package com.example.faceapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;



import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnSuccessListener<Text>, OnFailureListener {



    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;
    private ImageView mImageView;
    private TextView txtresults;
     Bitmap mSelectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtresults = findViewById(R.id.txtresults);
        mImageView = findViewById(R.id.image_view);

        if(checkSelfPermission(Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA},100);


    }

    public void abrirGaleria (View view){
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }
    public void abrirCamara (View view){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }
    public void OCRfx(View v) {

        if (mSelectedImage == null) {
            txtresults.setText("Seleccione una imagen primero");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void onFailure(@NonNull Exception e) {
        txtresults.setText("Error al procesar imagen");
    }

    @Override
    public void onSuccess(Text text) {

        List<Text.TextBlock> blocks = text.getTextBlocks();
        String resultados="";
        if (blocks.size() == 0) {
            resultados = "No hay Texto";
        }else{
            for (int i = 0; i < blocks.size(); i++) {
                List<Text.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    List<Text.Element> elements = lines.get(j).getElements();
                    for (int k = 0; k < elements.size(); k++) {
                        resultados = resultados + elements.get(k).getText() + " ";
                    }
                }
            }
            resultados=resultados + "\n";
        }

        //para que el textgo se ajuste
        txtresults.setMaxLines(15);
        txtresults.setEllipsize(null);
        txtresults.setMovementMethod(
                new android.text.method.ScrollingMovementMethod()
        );
        txtresults.setText(resultados);



    }




    public void Rostrosfx(View v)
    {
        if (mSelectedImage == null) {
            txtresults.setText("Seleccione una imagen primero");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();
        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.size() == 0) {
                            txtresults.setText("No Hay rostros");
                        }else{
                            txtresults.setText("Hay " + faces.size() + " rostro(s)");
                        }
                        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
                        Bitmap bitmap = drawable.getBitmap().copy(Bitmap.Config.ARGB_8888,true);
                        Canvas canvas = new Canvas(bitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(5);
                        paint.setStyle(Paint.Style.STROKE);
                        for (Face rostro:faces) {
                            canvas.drawRect(rostro.getBoundingBox(), paint);
                        }
                        mImageView.setImageBitmap(bitmap);
                    }
                })
                .addOnFailureListener(this);





    }




    public void Labeling(View v) {

        if (mSelectedImage == null) {
            txtresults.setText("Seleccione una imagen primero");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        String resultados = "";
                        for (ImageLabel label : labels)
                            resultados = resultados + label.getText() + " " + label.getConfidence() + "%\n";
                        txtresults.setText(resultados);
                    }
                })
                .addOnFailureListener(this);
    }

















    public void Barcodefx(View view) {
        if (mSelectedImage == null) {
            txtresults.setText("Seleccione una imagen primero");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        scanBarcode(image);
    }


    private void scanBarcode(InputImage image) {
        BarcodeScanner scanner = BarcodeScanning.getClient();
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    StringBuilder result = new StringBuilder();
                    for (Barcode barcode : barcodes) {
                        result.append("Tipo: ").append(barcode.getFormat()).append("\n");
                        result.append("Valor: ").append(barcode.getRawValue()).append("\n\n");
                    }
                    txtresults.setText(result.toString());
                })
                .addOnFailureListener(e -> txtresults.setText("Error al escanear: " + e.getMessage()));
    }
}