package com.dimlab.mapplace;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.List;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    private GoogleMap mMap;
    private Marker mMarker;

    private FusedLocationProviderClient mFusedLocationClient;

    private EditText searchEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);



        // Чтение пароля из EditText
        final EditText mPassword = findViewById(R.id.password);
        mPassword.setImeOptions(EditorInfo.IME_ACTION_SEND);
        mPassword.setRawInputType(InputType.TYPE_CLASS_TEXT);
        Button mEnter = findViewById(R.id.btm_enter);

        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mEnter.performClick(); // Имитация нажатия на кнопку

                    // Скрыть клавиатуру
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mPassword.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        // Обработчик входа в приложение

        mEnter.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                File mFile = new File(getFilesDir(), "hash.txt");
                if(!mFile.exists())
                {
                    writeHashToFile(getHash("tur"));
                }

                String input = mPassword.getText().toString();
                // Хэширование пароля
                String hash = getHash(input);
                String expectedHash = readHashFromFile();

                if (hash.equals(expectedHash))
                {
                    ConstraintLayout mMapScreen = findViewById(R.id.map_screen);
                    mMapScreen.setVisibility(View.VISIBLE);
                    ConstraintLayout mEnterScreen = findViewById(R.id.enter_screen);
                    mEnterScreen.setVisibility(View.INVISIBLE);
                    RelativeLayout mSearch = findViewById(R.id.search_layout);
                    mSearch.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Добро пожаловать!!", Toast.LENGTH_SHORT).show();
                    //StartApp();
                } else {
                    ConstraintLayout mMapScreen = findViewById(R.id.map_screen);
                    mMapScreen.setVisibility(View.INVISIBLE);
                    ConstraintLayout mEnterScreen = findViewById(R.id.enter_screen);
                    mEnterScreen.setVisibility(View.VISIBLE);
                    RelativeLayout mSearch = findViewById(R.id.search_layout);
                    mSearch.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, "Пароль неверный", Toast.LENGTH_SHORT).show();
                }
            }

            private void StartApp()
            {
                try {
                    File file = new File(getFilesDir(), "data.txt");
                    //создаем объект FileReader для объекта File
                    FileReader fr = new FileReader(file);
                    //создаем BufferedReader с существующего FileReader для построчного считывания
                    BufferedReader reader = new BufferedReader(fr);
                    // считаем сначала первую строку, где храниться пароль
                    String line = reader.readLine();
                    while (line != null)
                    {
                        // Считывание строки
                        String[] parts = line.split(", ");
                        double latitude = Double.parseDouble(parts[0].trim());
                        double longitude = Double.parseDouble(parts[1].trim());
                        LatLng markerLatLng = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions().position(markerLatLng)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        line = reader.readLine();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }  catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Хэширование пароля
            public String getHash(String input)
            {
                try
                {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashBytes = md.digest(input.getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hashBytes)
                    {
                        sb.append(String.format("%02x", b));
                    }
                    return sb.toString();
                } catch (NoSuchAlgorithmException e)
                {
                    Toast.makeText(MainActivity.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();
                    return null;
                }
            }

            // Запись хэша в файл
            private void writeHashToFile(String hash)
            {
                try
                {
                    File file = new File(getFilesDir(),"hash.txt");
                    FileOutputStream outputStream = new FileOutputStream(file);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

                    writer.write(hash);
                    writer.close();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            // Получения хэша из файла
            private String readHashFromFile()
            {
                try
                {
                    File file = new File(getFilesDir(), "hash.txt");
                    InputStream inputStream = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String hash = reader.readLine();
                    reader.close();
                    return hash;
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
                    return null;
                }
            }
        });

        // Обработчик нажатия на кнопку открытия окна смены пароля
        Button mChangeOpen = findViewById(R.id.btm_change_password);
        mChangeOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ConstraintLayout mEnterScreen = findViewById(R.id.enter_screen);
                mEnterScreen.setVisibility(View.INVISIBLE);
                ConstraintLayout mChangePassword = findViewById(R.id.change_passord);
                mChangePassword.setVisibility(View.VISIBLE);
            }
        });

        // Обработчик нажатия на кнопку отмены смены пароля
        Button mChangeCanel = findViewById(R.id.btm_change_canel);
        mChangeCanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ConstraintLayout mEnterScreen = findViewById(R.id.enter_screen);
                mEnterScreen.setVisibility(View.VISIBLE);
                ConstraintLayout mChangePassword = findViewById(R.id.change_passord);
                mChangePassword.setVisibility(View.INVISIBLE);
            }
        });

        // Чтение пароля из EditText
        final EditText mOldpass = findViewById(R.id.oldpass);
        mOldpass.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPassword.setRawInputType(InputType.TYPE_CLASS_TEXT);
        final EditText mNewpass = findViewById(R.id.newpass);
        mNewpass.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPassword.setRawInputType(InputType.TYPE_CLASS_TEXT);

        // Обработчик нажатия на кнопку смены пароля
        Button mChange = findViewById(R.id.btm_change);
        mChange.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                String input = mOldpass.getText().toString();
                // Хэширование пароля
                String hash = getHash(input);
                String expectedHash = readHashFromFile();

                if (hash.equals(expectedHash))
                {
                    // Чтение пароля из EditText
                    input = mNewpass.getText().toString();
                    // Хэширование и сохранение пароля
                    writeHashToFile(getHash(input));
                    // Возвращаемся к экрану авторизации
                    ConstraintLayout mEnterScreen = findViewById(R.id.enter_screen);
                    mEnterScreen.setVisibility(View.VISIBLE);
                    ConstraintLayout mChangePassword = findViewById(R.id.change_passord);
                    mChangePassword.setVisibility(View.INVISIBLE);
                }else{
                    Toast.makeText(MainActivity.this, "Пароль неверный", Toast.LENGTH_SHORT).show();
                }
            }

            // Хэширование пароля
            public String getHash(String input)
            {
                try
                {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashBytes = md.digest(input.getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hashBytes)
                    {
                        sb.append(String.format("%02x", b));
                    }
                    return sb.toString();
                } catch (NoSuchAlgorithmException e) {
                    Toast.makeText(MainActivity.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            // Запись хэша в файл
            private void writeHashToFile(String hash)
            {
                try
                {
                    File file = new File(getFilesDir(), "hash.txt");
                    FileOutputStream outputStream = new FileOutputStream(file, false);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                    writer.write(hash);
                    writer.close();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            // Получения хэша из файла
            private String readHashFromFile()
            {
                try
                {
                    File file = new File(getFilesDir(), "hash.txt");
                    InputStream inputStream = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String hash = reader.readLine();
                    reader.close();
                    return hash;
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
                    return null;
                }
            }
        });

        // Инициализация карты
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Инициализация FusedLocationProviderClient для получения местоположения
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Инициализация поля ввода для поиска адреса
        searchEditText = findViewById(R.id.input_search);

        searchEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);
        Button searchButton = findViewById(R.id.search_button);

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    searchButton.performClick(); // Имитация нажатия на кнопку

                    // Скрыть клавиатуру
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        // Обработчик нажатия на кнопку поиска адреса

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String address = searchEditText.getText().toString();
                if (!address.isEmpty())
                {
                    searchAddress(address);
                }
            }
        });
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    // Получение текущего местоположения
    private void getCurrentLocation() {
        if (checkPermissions()) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null)
                    {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                    }
                }
            });
        }
        else {
            Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    // Поиск адреса по строке запроса
    private void searchAddress(String address) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address firstAddress = addresses.get(0);
                double latitude = firstAddress.getLatitude();
                double longitude = firstAddress.getLongitude();
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            } else {
                Toast.makeText(this, "Адрес не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при поиске адреса", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        // Проверяем разрешение на доступ к местоположению
        if (checkPermissions()) {
            // Разрешение уже предоставлено, отображаем текущее местоположение на карте
            mMap.setMyLocationEnabled(true);
        } else {
            // Запрашиваем разрешение на доступ к местоположению
            requestPermissions();
        }

        // Установка обработчика нажатия на метку
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker)
            {
                ConstraintLayout mPicScreen = findViewById(R.id.pic_screen);
                mPicScreen.setVisibility(View.VISIBLE);
                RelativeLayout mSearch = findViewById(R.id.search_layout);
                mSearch.setVisibility(View.INVISIBLE);
                /*ImageView mImage;
                mImage = (ImageView) findViewById(R.id.users_picture);
                mImage.setImageBitmap(BitmapFactory.decodeFile());*/
                // Действия при нажатии на метку
                /*Toast.makeText(getApplicationContext(),
                        "Вы нажали на метку " + marker.getTitle(),
                        Toast.LENGTH_SHORT).show();*/

                // Обработчик нажатия на кнопку "Удалить маркер"
                Button mDelPic = findViewById(R.id.btm_del_marker);

                mDelPic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMarker.remove();
                        ConstraintLayout mPicScreen = findViewById(R.id.pic_screen);
                        mPicScreen.setVisibility(View.INVISIBLE);
                        RelativeLayout mSearch = findViewById(R.id.search_layout);
                        mSearch.setVisibility(View.VISIBLE);
                    }
                });

                // Обработчик нажатия на кнопку "Закрыть фото"
                Button mClose = findViewById(R.id.btm_close_pic);
                mClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        ConstraintLayout mPicScreen = findViewById(R.id.pic_screen);
                        mPicScreen.setVisibility(View.INVISIBLE);
                        RelativeLayout mSearch = findViewById(R.id.search_layout);
                        mSearch.setVisibility(View.VISIBLE);
                    }
                });
                return false;
            }
        });

        // Установка обработчика нажатия на карту для добавления метки
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng)
            {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Установить метку?");
                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // Открытие галереи
                        pickImageFromGallery();
                    }
                });
                builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Здесь выполняются действия по отмене
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });

        // Инициализация поля для поиска адреса
        searchEditText = findViewById(R.id.input_search);

        // Обработчик нажатия на кнопку поиска
        ImageView searchButton = findViewById(R.id.ic_magnifier);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchString = searchEditText.getText().toString();
                if (!searchString.isEmpty()) {
                    Geocoder geocoder = new Geocoder(MainActivity.this);
                    try {
                        List<Address> addressList = geocoder.getFromLocationName(searchString, 1);

                        if (!addressList.isEmpty()) {
                            Address address = addressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        } else {
                            Toast.makeText(MainActivity.this, "Адрес не найден", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // Проверка разрешений на доступ к местоположению
    private boolean checkPermissions() {
        int permissionState = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    // Запрос разрешений на доступ к местоположению
    private void requestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    // Обработка ответа на запрос разрешений на доступ к местоположению
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "Разрешение на доступ к местоположению не предоставлено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Выбор фото из галереи
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    // Получение пути выбранной фотографии в методе onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(mMap.getCameraPosition().target)
                    .snippet(picturePath)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));


            LatLng position = mMarker.getPosition();
            String coordinates = position.latitude + ", " + position.longitude + ", " + picturePath;

            writeToFile(coordinates);



            /*Toast.makeText(getApplicationContext(),
                    "Координаты: " + position.latitude + ", " + position.longitude,
                    Toast.LENGTH_SHORT).show();*/

            // В переменной picturePath будет лежать путь до выбранной фотографии
        }
    }

    private void writeToFile(String input)
    {
        try
        {
            File file = new File(getFilesDir(),"data.txt");
            FileOutputStream outputStream = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            writer.write(input);
            writer.close();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
        }
    }
}

