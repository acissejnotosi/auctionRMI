/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leilao;

import java.awt.AWTException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

/**
 *
 * @author allan
 */
public class InitSystem {

    static ArrayList<Process> processList = new ArrayList<>();

    static PublicKey chave_publica = null;

    public static void main(String[] args) throws InterruptedException, AWTException, NoSuchAlgorithmException, InvalidKeySpecException, UnknownHostException, IOException {

        int PORT_MULTICAST = 6789;
        String IP_MULTICAST = "228.5.6.7";
        MulticastSocket s = null;
        DatagramSocket socket = null;
        Process process = null;
        GeraChave gera_chave = null;
        PrivateKey chave_privada = null;

        InetAddress group = InetAddress.getByName(IP_MULTICAST);
        s = new MulticastSocket(PORT_MULTICAST);
        s.joinGroup(group);
        socket = new DatagramSocket();

        // ********************************************
        // Receiving data from user
        String id;
        String port;
        String nomeProduto;
        String idProduto;
        String descProduto;
        String precoProduto;
        String tempoFinal = "120000"; //para que cada produto tenha um tempo de leilão de 2 min

        Scanner in = new Scanner(System.in);
        System.out.println("Informe o número do participante:");
        id =  in.nextLine();

        System.out.println("Informe a porta para comunicação UNICAST:");
        port = in.nextLine();

        System.out.println("Informe o nome do produto:");
        nomeProduto = in.nextLine();

        System.out.println("Informe o id do produto:");
        idProduto = in.nextLine();

        System.out.println("Informe descricao do produto:");
        descProduto = in.nextLine();

        System.out.println("Informe o preço do produto:");
        precoProduto = in.nextLine();

        // ********************************************
        // Generating keys for this process
        gera_chave = new GeraChave();
        gera_chave.geraChave();
        chave_privada = gera_chave.getChavePrivada();
        chave_publica = gera_chave.getChavePublica();

        process = new Process(id, port, chave_publica, nomeProduto, idProduto, descProduto, precoProduto);

        InitSystem.processList.add(process);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(10);
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeChar('N');
        oos.writeUTF(id);
        oos.writeUTF(port);
        oos.writeObject(chave_publica);
        oos.writeUTF(nomeProduto);
        oos.writeUTF(idProduto);
        oos.writeUTF(descProduto);
        oos.writeUTF(precoProduto);
        oos.flush();

        // *********************************************
        // Initializing multicast and unicast communication
        ReadingThread multCastComm = new ReadingThread(process, IP_MULTICAST, PORT_MULTICAST);
        multCastComm.start();
        UniCastServer uniCastComm = new UniCastServer(process, IP_MULTICAST, PORT_MULTICAST);
        uniCastComm.start();

        // *********************************************
        // Sending multicast notification of its presence.
        byte[] m = bos.toByteArray();
        DatagramPacket messageOut = new DatagramPacket(m, m.length, group, PORT_MULTICAST);

        System.out.println("\n[MULTICAST SEND] Sending information about this new process:");
        System.out.print("[MULTICAST SEND]");
        System.out.print(" ID do participante: " + id);
        System.out.print(", Porta: " + port);
        System.out.print(", Chave publica: - ");
        System.out.print(", Nome produto: " + nomeProduto);
        System.out.println(",ID Produto: " + idProduto);
        System.out.print(",Descricao do produto: " + descProduto);
        System.out.println(",Preco do produto: " + precoProduto);
        s.send(messageOut);

        // *********************************************
        // Interaction phase.
        while (true) {
            String cmd;

            System.out.println("MENU");
            System.out.println("Pressione a tecla desejada:");
            System.out.println("[P] Leiloar produto ");
            System.out.println("[L] Lista os processos ");
            System.out.println("[T] Listar transacoes efetuadas ");
            System.out.println("[E] to Exit");
            cmd = in.nextLine().trim().toUpperCase();
            System.out.println("");

            Iterator it;
            switch (cmd) {

                case "B":
                    // check if there are more than 2 active processes
                    if (InitSystem.processList.size() <= 2) {
                        System.out.println("Must have at least 4 ative processes to buy coins");
                        break;
                    }

                    //  byte[] m = bos.toByteArray();
                    // DatagramPacket messageOut = new DatagramPacket(m, m.length, group, PORT_MULTICAST);
                    System.out.println("\n[MULTICAST SEND] Sending information about this new process:");
                    System.out.print("[MULTICAST SEND]");
                    System.out.print(" ID: " + id);
                    System.out.print(", Port: " + port);
                    System.out.print(", Public Key: Intern");
                    //      System.out.print(", Coin Amount: " + coinAmount);
                    //     System.out.println(", Coin Price: " + coinPrice);
                    s.send(messageOut);

                    break;
                case "L":
                    System.out.println("List of Process:");
                    it = InitSystem.processList.iterator();
                    while (it.hasNext()) {
                        Process p = (Process) it.next();
                        System.out.println(p.imprimaProcessos());
                    }
                    break;
                case "E":
                    System.out.println("Bye!");
                    s.leaveGroup(group);
                    s.close();
                    System.exit(0);
            }
        }
    }
}
