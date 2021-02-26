package com.example.myapplication;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import android.app.Activity;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View.OnTouchListener;
import android.graphics.Color;
import android.util.Log;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.opencv.imgproc.Imgproc;

public class MainActivity extends CameraActivity implements SensorEventListener, CvCameraViewListener2 {

    String SERVER_IP = "192.168.1.23";


    //Variabili gestione loop
    private boolean winning = false;
    private boolean write_yes = false;
    private String old_Message = "s";
    private Boolean control_move = false;
    boolean sovrascrivi = false;
    private int score = 0;
    private boolean running = false;

    //variabili sensore movimento
    private SensorManager sensorManager;
    private Sensor accelerometer;
    //private long lastUpdate;
    public int y;

    //variabili socket
    private boolean autoInc = false;
    private final long REPEAT_DELAY = 50;
    private DataOutputStream output;
    int SERVER_PORT = 8080;
    Thread Thread1 = null;
    Thread Thread3 = null;
    boolean connection = false;

    //Variabili gestione camera
    private static final String TAG = "ColorBlobDetectActivity";
    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;
    private CameraBridgeViewBase mOpenCvCameraView;

    //----------------------------CREAZIONE E GESTIONE-------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        //-------------------------------------------------------------------------------------
        //--------------------inizializzazione variabili sensori-------------------------------
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //lastUpdate = System.currentTimeMillis();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //-------------------------------------------------------------------------------------

        Log.i(TAG, "called OnCreate");

        runTimer();

        final TextView connect = (TextView) findViewById(R.id.connessione);
        connect.setText("NON CONNESSO");

        //avvio connessione con server
        Thread1 = new Thread(new Thread1());
        Thread1.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,
                    this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    //--------------------------SENSORE MOVIMENTO------------------------------

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            //variabile contenente l'informazione dell'inclinazione del telefono
            y = (int) event.values[1];

        }
    }

    //-----------------------------BOTTONI----------------------------------------

    public void OnClickAcc(View view) {
        String message;
        //si avvia il gioco solo se ci si è connessi al server
        if(connection) {
            //se si clicca sul pulsante avanti dopo la connessione il gioco si avvia (running --> true)
            running = true;

            //trasmissione messaggio "avanti" al server
            message = "w";
            old_Message = "w";
            control_move = true;
            Thread3 = new Thread(new Thread3(message));
            Thread3.start();
        }
    }

    public void OnClickBrk(View view) {
        String message;
        if(connection) {
            //trasmissione messaggio "fermati" al server
            message = "s";
            old_Message = "s";
            control_move = false;
            Thread3 = new Thread(new Thread3(message));
            Thread3.start();
        }
    }

    //-----------------------------------TX----------------------------------------------

    class Thread1 implements Runnable {
        Socket socket;

        @Override
        public void run() {
            while(!connection) {
                try {
                    socket = new Socket(InetAddress.getByName(SERVER_IP), SERVER_PORT);
                    output = new DataOutputStream(socket.getOutputStream());
                    connection = true;
                    new Thread(new Thread2()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //---------------------elaborazione messaggio "curva" o "avanti" da trasmettere al server---------------------------
    class Thread2  implements Runnable {

        @Override
        public void run() {
            while (true) {
                if(connection) {
                    //per evitare di creare un numero eccessivo di thread intasando il sistema si esegue un controllo
                    // sullo stato del sistema. Si trasmette solo quando lo stato cambia

                    //destra
                    if (y >= 4 && !old_Message.equals("d") && control_move) {
                        Thread3 = new Thread(new Thread3("d"));
                        Thread3.start();
                        old_Message = "d";
                        //sinistra
                    } else if (y <= -4 && !old_Message.equals("a") && control_move) {
                        Thread3 = new Thread(new Thread3("a"));
                        Thread3.start();
                        old_Message = "a";
                    }
                    //dritto
                    if (y > -4 && y < 4 && (old_Message.equals("a") || old_Message.equals("d"))) {
                        if (control_move) {
                            Thread3 = new Thread(new Thread3("w"));
                            Thread3.start();
                            old_Message = "w";
                        }
                    }
                }
            }

        }
    }

    //--------------------------------------trasmissione messaggio al server-----------------------------------------
    class Thread3 implements Runnable {
        private String message;

        Thread3(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                output.writeBytes(message);
                output.writeBytes("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //-------------------------------------------METODI CAMERA--------------------------------------------------------

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        return  new Scalar(pointMatRgba.get(0, 0));
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status)  {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.i(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(),
                    70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    //--------------------------------SCRITTURA SUL FILE TXT DEL TELEFONO---------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void writeToFile(String content) {
        String path =
                Environment.getExternalStorageDirectory() + File.separator  + "EsameHCI";
        //Creazione variabile "cartella"
        File folder = new File(path);

        //se la cartella non esiste la si crea
        folder.mkdirs();

        //Creazione variabile "file"
        File file = new File(folder, "Punteggi.txt");

        try {
            //si verifica l'esistenza del file e nel caso non esista lo si crea
            if(!file.exists())
                file.createNewFile();

            //si salvano le informazioni contenute nel file
            String x = new String(Files.readAllBytes(file.toPath()));

            //si genera la variabile OutputStreamWriter per permettere la scrittura su file
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            //la scrittura formatta tutto ciò che era scritto all'interno del file, quindi si procede con la riscrittura
            // di ciò che era stato in precedenza letto aggiungendovi il nuovo punteggio
            myOutWriter.append(x+"\n" + content);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public String getPath(){
        return Environment.getExternalStorageDirectory() + File.separator  + "EsameHCI" + File.separator  + "Punteggi.txt";
    }

    //---------------------METODI RUN------------------

    private void runTimer() {
        final TextView timeView = (TextView)findViewById(R.id.time_view);
        timeView.setText(score + " ");
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                if(connection && !winning){
                    final TextView connect = (TextView) findViewById(R.id.connessione);
                    connect.setText("CONNESSO");
                }
                if (!winning) {
                    try {
                        //gestione rilevamento colore
                        int cols = mRgba.cols();
                        int rows = mRgba.rows();

                        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
                        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

                        int x_imm = 2160 / 2 - xOffset;
                        int y_imm = 1080 / 2 - yOffset;

                        if ((x_imm < 0) || (y_imm < 0) || (x_imm > cols) || (y_imm > rows)) running = false;
                        else {
                            Rect centerRect = new Rect();
                            centerRect.x = (x_imm > 4) ? x_imm - 4 : 0;
                            centerRect.y = (y_imm > 4) ? y_imm - 4 : 0;
                            centerRect.width = (x_imm + 4 < cols) ? x_imm + 4 - centerRect.x : cols - centerRect.x;
                            centerRect.height = (y_imm + 4 < rows) ? y_imm + 4 - centerRect.y : rows - centerRect.y;

                            Mat centerRegionRgba = mRgba.submat(centerRect);
                            Mat centerRegionHsv = new Mat();
                            Imgproc.cvtColor(centerRegionRgba, centerRegionHsv, Imgproc.COLOR_RGB2HLS_FULL);

                            mBlobColorHsv = Core.sumElems(centerRegionHsv);
                            int pointCount = centerRect.width * centerRect.height;
                            for (int i = 0; i < mBlobColorHsv.val.length; i++)
                                mBlobColorHsv.val[i] /= pointCount;
                            mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);
                            Log.i(TAG, "Center rgba color: (" + mBlobColorRgba.val[0] + ", "
                                    + mBlobColorRgba.val[1] + ", "
                                    + mBlobColorRgba.val[2] + ", "
                                    + mBlobColorRgba.val[3] + ")");

                            mDetector.setHsvColor(mBlobColorHsv);
                            Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE,
                                    0, 0, Imgproc.INTER_LINEAR_EXACT);
                            mIsColorSelected = true;
                            centerRegionRgba.release();
                            centerRegionHsv.release();
                        }
                        //se il gioco è stato avviato e la telecamera è puntata sul percorso (quindi sul rosso)
                        // allora si incrementa il punteggio
                        if (running && mBlobColorRgba.val[0] > 230) {
                            score++;
                            timeView.setText(score + " ");
                        }
                        //se il gioco è stato avviato e la telecamera è puntata sul traguardo (quindi sul verde)
                        //il gioco termina
                        if (running && mBlobColorRgba.val[1] > 230 && mBlobColorRgba.val[2] > 130 && mBlobColorRgba.val[2] < 170)
                            winning = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else { // --> if(winning)
                    //LINE DI CODICE ESEGUITE SOLO IN CASO DI VITTORIA

                    //si scrive sul file txt contenente la lista dei punteggi il nuovo score
                    if (!sovrascrivi) {
                        String write = "" + score;

                        writeToFile(write);
                        sovrascrivi = true;

                        //si cambia schermata passando alla win activity
                        setContentView(R.layout.win_activity);
                    } else {
                        if (!write_yes) {
                            try {
                                //si esegue la lettura del file txt contenente i punteggi e li si riscrivere sulle
                                //textview desiderate

                                String path =
                                        Environment.getExternalStorageDirectory() + File.separator + "EsameHCI";

                                File folder = new File(path);

                                // Create the file.
                                File file = new File(folder, "Punteggi.txt");

                                String x = new String(Files.readAllBytes(file.toPath()));

                                //la lettura del file salva tutti i punteggi in una unica string.
                                //Visto che i punteggi sono separati dal carattere divisore a capo ("\n") è possibile
                                //separarli e salvarli in un array utilizzando il metodo split
                                String[] parts = x.split("\n");

                                String past = "";
                                int count = 0;

                                //si salvano tutti i punteggi eccetto l'ultimo (perché quello attuale) all'interno della
                                //textview contenente i punteggi passati
                                while (count < parts.length - 1) {
                                    past = past + "\n" + parts[count];
                                    count++;
                                }
                                TextView tw_lp = findViewById(R.id.ListaPunteggi);
                                tw_lp.setText(past);
                                tw_lp.setMovementMethod(new ScrollingMovementMethod());

                                //si stampa il punteggio attuale
                                TextView tw_score = findViewById(R.id.ScorePoint);
                                tw_score.setText(tw_score.getText() + parts[parts.length - 1]);

                                write_yes = true;

                            } catch (IOException e) {

                            }
                        }

                    }
                }
                //il loop viene eseguito ogni 500 millisecondi
                handler.postDelayed(this, 500);
            }
        });
    }
}