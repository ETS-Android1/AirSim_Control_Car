package com.pervasive;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Server1 {
    private static Thread Thread1;
    static ServerSocket serverSocket;
    private static DataOutputStream output;
    private static BufferedReader input;

    private static String old_command = "e";

    public static final int SERVER_PORT = 8080;

    public static InputStream is;
    public static InputStreamReader isr;
    public static BufferedReader br;
    private static Thread Thread4;
    private static int count;
    private static Process process;
    private static BufferedWriter writer;
    private static BufferedReader reader;

    public static void main(String[] args) throws IOException, InterruptedException {
        count = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        //il thread1 si occupa di connettere il server con l'eventuale client
        Thread1 = new Thread(new Thread1());
        Thread1.start();

        System.out.printf("Output of running %s is:", Arrays.toString(args));

        while (true) {
            //tramite la funzione message si verifica la ricezione di un messaggio e nel caso si avvio il thread2 che
            //lo trasmetterà al simulatore
            String message = in.readLine();
            if(message!= null) {
                new Thread(new Thread2()).start();
            }
        }
    }


    //----------------------------------------CONNESSIONE CON CLIENT----------------------------------------------------
    static class Thread1 implements Runnable {
        //otteniamo l'indirizzo ip
        InetAddress inetAddress = InetAddress.getLocalHost();

        Thread1() throws UnknownHostException {
        }
        @Override
        public void run() {
            Socket socket;
            try {
                //si apre la connessione sulla porta 8080
                serverSocket = new ServerSocket(SERVER_PORT);
                System.out.println("Not connected!\n" + "\nPort: " + SERVER_PORT + "\n");
                try {
                    socket = serverSocket.accept();

                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    System.out.println("Connected\n");

                    //si avvia il programma che gestisce il simulatore
                    if(count == 0) {
                        count = 1;
                        Run_Program("E:\\passare\\DOWNLOAD\\API_controller.exe", "\n");
                    }

                    new Thread(new Thread2()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //il thread2 si occupa della lettura del messaggio in input dal client e procede a trasmetterlo al programma che
    //gestisce il simulatore
    private static class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    //leggo il messaggio
                    final String message = input.readLine();
                    if (message != null) {
                        old_command = message;
                        System.out.println("Client: " + message + "\n");

                        //lo trasmetto
                        writer.write(message + "\n");
                        writer.flush();
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //crea il programma che gestisce il simulatore e la variabile writer che permette di trasmettere le informazioni
    public static void Run_Program(String command, String input) throws IOException, InterruptedException {
        // creiamo il processo
        String[] argss = {"cmd", "/c", command};
        ProcessBuilder pb = new ProcessBuilder(argss);
        process = pb.start();
        // si creano le variabili che permettono la lettura e scrittura dei messaggi in input e output dal processo
        //creato
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        //Si scrive un il messaggio di input
        writer.write(input + "\n");
        writer.flush();

        //fase di reader
        String line = "";
        int c = 0;

        //nel caso in cui il programma .exe si connettesse con il simulatore darebbe un riscontro all'utente, dunque lo
        //si legge e stampa a terminale
        while (c <6) {
            line = reader.readLine();
            c++;
            System.out.println(line);
        }
        // si procede con la chiusura del reader visto che l'exe non trasmetterà più nulla
        reader.close();
    }
}