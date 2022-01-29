package hu.petrik.qrdolgozat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Button btnScan, btnKiir, btnKoord;
    private TextView textViewSzoveg;
    private ImageView imageViewKoordQr;
    private boolean writePermission;
    private double longitude;
    private double latitude;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                intentIntegrator.setPrompt("QR Code Scanner");
                intentIntegrator.setBeepEnabled(false);
                intentIntegrator.initiateScan();
            }
        });

        btnKiir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {

                    writePermission = false;
                    String[] permissions =
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, 0);
                } else{
                    writePermission = true;
                }
                if (writePermission) {
                    Date date = Calendar.getInstance().getTime();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String s = String.format("%s, %s, %.2f, %.2f", textViewSzoveg.getText().toString(), format.format(date), longitude, latitude);

                    try {
                        FileOutputStream fileOutputStream = openFileOutput("scannedCodes.csv", MODE_PRIVATE);
                        fileOutputStream.write(s.getBytes());

                        Toast.makeText(MainActivity.this, "Sikeresen elmentve: " + getFilesDir() + "/scannedCodes.csv", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnKoord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = String.format("%.2f;%.2f",longitude, latitude);
                MultiFormatWriter mfw = new MultiFormatWriter();
                try {
                    BitMatrix bitMatrix = mfw.encode(text, BarcodeFormat.QR_CODE, 500,500);
                    BarcodeEncoder bce = new BarcodeEncoder();
                    Bitmap bitmap = bce.createBitmap(bitMatrix);
                    imageViewKoordQr.setImageBitmap(bitmap);
                }catch (WriterException e){
                    e.printStackTrace();
                }
                //Toast.makeText(MainActivity.this, "Longitude:"+Double.toString(longitude)+"\nLatitude:"+Double.toString(latitude), Toast.LENGTH_SHORT).show();
                textViewSzoveg.setText(String.format("%.2f;%.2f",longitude, latitude));
            }
        });
    }

    public void init() {
        btnScan = findViewById(R.id.btnScan);
        btnKiir = findViewById(R.id.btnKiir);
        btnKoord = findViewById(R.id.btnKoord);
        imageViewKoordQr = findViewById(R.id.imageViewKoordQr);
        textViewSzoveg = findViewById(R.id.textViewSzoveg);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = location -> {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            String[] permissions =
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    };
            ActivityCompat.requestPermissions(this, permissions, 1);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0, 0, locationListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Kiléptél a scannelésből", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    if(Patterns.WEB_URL.matcher(result.getContents()).matches()) {
                        AlertDialog alertDialog;
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Megnyitod a linket:\n" + result.getContents());
                        builder.setPositiveButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                textViewSzoveg.setText(result.getContents());
                            }
                        });
                        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Uri uri = Uri.parse(result.getContents());
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                                textViewSzoveg.setText(result.getContents());
                            }
                        });
                        builder.setCancelable(false);
                        alertDialog = builder.create();
                        alertDialog.show();
                    }
                    textViewSzoveg.setText(result.getContents());
                } catch (Exception e) {
                    Log.d("URI ERROR", e.toString());
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED)
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0, 0, locationListener);
            }
        }
        if (requestCode == 0) {
            writePermission =
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}