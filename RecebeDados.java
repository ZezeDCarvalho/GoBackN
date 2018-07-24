/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recepcao;

/**
 *
 * @author flavio
 */
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecebeDados extends Thread {

    private final int portaLocalReceber = 2001;
    private final int portaLocalEnviar = 2002;
    private final int portaDestino = 2003;
    private final double probalidadePerdaPct = 0.999;
    private Random aleatorio = new Random();

    private void enviaAck(boolean fim, int cabecalho) {
        try {
            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnviar)) {
                String sendString = Integer.toString(cabecalho);
                if (fim) {
                    sendString = "-1";
                }
                byte[] sendData = sendString.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length, address, portaDestino);

                datagramSocket.send(packet);
            }
        } catch (SocketException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(portaLocalReceber);
            byte[] receiveData = new byte[1400];
            try (FileOutputStream fileOutput = new FileOutputStream("saida")) {
                boolean fim = false;
                while (!fim) {
                    double probabilidade = aleatorio.nextDouble();
                    if (probalidadePerdaPct <= probabilidade) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);

                        byte[] tmp = receivePacket.getData();

                        int cabecalho = (((int) tmp[0]) << 12) + (((int) tmp[1]) << 8) + (((int) tmp[2]) << 4) + ((int) tmp[3]);

                        System.out.println("dado recebido "+ cabecalho);

                        //probabilidade de 60% de perder
                        //gero um numero aleatorio contido entre [0,1]
                        //se numero cair no intervalo [0, 0,6)
                        //significa perda, logo, você não envia ACK
                        //para esse pacote, e não escreve ele no arquivo saida.
                        //se o numero cair no intervalo [0,6, 1,0]
                        //assume-se o recebimento com sucesso.
                        for (int i = 4; i < tmp.length; i = i + 4) {
                            int dados = ((tmp[i] & 0xff) << 24) + ((tmp[i + 1] & 0xff) << 16) + ((tmp[i + 2] & 0xff) << 8) + ((tmp[i + 3] & 0xff));

                            if (dados == -1) {
                                fim = true;
                                break;
                            }
                            fileOutput.write(dados);
                        }
                        enviaAck(fim, cabecalho);
                    } else {
                        System.out.println("dado não recebido");
                    }
                        
                }
            }
        } catch (IOException e) {
            System.out.println("Excecao: " + e.getMessage());
        }
    }
}