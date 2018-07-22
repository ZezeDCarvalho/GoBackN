package envio;

/**
 * @author flavio
 */
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Semaphore;

import java.net.SocketTimeoutException;

public class EnviaDados extends Thread {

    private final int portaLocalEnvio = 2000;
    private final int portaDestino = 2001;
    private final int portaLocalRecebimento = 2003;
    Semaphore sem;
    private final String funcao;
    private final int tempoTimeout = 1000;
    private int cabecalho = 0;
    private int cbUltimoAck = 0;

    public EnviaDados(Semaphore sem, String funcao) {
        super(funcao);
        this.sem = sem;
        this.funcao = funcao;
    }

    public String getFuncao() {
        return funcao;
    }

    private void enviaPct(int[] dados) {
        //converte int[] para byte[]
        ByteBuffer byteBuffer = ByteBuffer.allocate(dados.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(dados);

        byte[] buffer = byteBuffer.array();

        try {
            System.out.println("Semaforo: " + sem.availablePermits());
            sem.acquire();
            System.out.println("Semaforo: " + sem.availablePermits());

            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnvio)) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, portaDestino);
                datagramSocket.send(packet);
            }

            System.out.println("Envio feito.");
        } catch (SocketException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void reEnviaPct(String idAck) {
        //variavel onde os dados lidos serao gravados
        // M: arrumar esse método. Possivelmente transformar em um arraylist com endereços ou algo próximo.
            int[] dados = new int[350];
            //contador, para gerar pacotes com 1400 Bytes de tamanho
            //como cada int ocupa 4 Bytes, estamos lendo blocos com 350
            //int's por vez.
            int cont = 0;

            try (FileInputStream fileInput = new FileInputStream("entrada");) {
                int lido;
                while ((lido = fileInput.read()) != -1) {
                    if (cont == 0){
                        dados[cont++] = ++cabecalho;
                    }
                    dados[cont] = lido;
                    cont++;
                    if (cont == 350) {
                        //envia pacotes a cada 350 int's lidos.
                        //ou seja, 1400 Bytes.
                        if (Integer.toString(this.cabecalho).equals(idAck)) {
                            System.out.println("Re-enviando pacote: "+this.cabecalho);
                            enviaPct(dados);
                            break;
                        }
                        cont = 0;
                    }
                }

                //ultimo pacote eh preenchido com
                //-1 ate o fim, indicando que acabou
                //o envio dos dados.
                for (int i = cont; i < 350; i++) {
                    dados[i] = -1;
                }
                System.out.println("Sequência: "+cabecalho);
                enviaPct(dados);
            } catch (IOException e) {
                System.out.println("Error message: " + e.getMessage());
            }
    }

    @Override
    public void run() {

        switch (this.getFuncao()) {
            case "envia":
                //variavel onde os dados lidos serao gravados
                int[] dados = new int[350];
                //contador, para gerar pacotes com 1400 Bytes de tamanho
                //como cada int ocupa 4 Bytes, estamos lendo blocos com 350
                //int's por vez.
                int cont = 0;

                try (FileInputStream fileInput = new FileInputStream("entrada");) {
                    int lido;
                    while ((lido = fileInput.read()) != -1) {
                        if (cont == 0){
                            dados[cont++] = ++cabecalho;
                        }
                        dados[cont] = lido;
                        cont++;
                        if (cont == 350) {
                            //envia pacotes a cada 350 int's lidos.
                            //ou seja, 1400 Bytes.
                            System.out.println("Sequência: "+this.cabecalho);
                            enviaPct(dados);
                            cont = 0;
                        }
                    }

                    //ultimo pacote eh preenchido com
                    //-1 ate o fim, indicando que acabou
                    //o envio dos dados.
                    for (int i = cont; i < 350; i++) {
                        dados[i] = -1;
                    }
                    System.out.println("Sequência: "+cabecalho);
                    enviaPct(dados);
                } catch (IOException e) {
                    System.out.println("Error message: " + e.getMessage());
                }
                break;
		case "ack":
		try {
			DatagramSocket serverSocket = new DatagramSocket(portaLocalRecebimento);
			byte[] receiveData = new byte[4];
			String retorno = "";
            serverSocket.setSoTimeout(tempoTimeout);
			while (!retorno.equals("-1")) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        serverSocket.receive(receivePacket);
                        retorno = new String(receivePacket.getData());
                        System.out.println("Ack recebido "+ retorno +".");
                        //serverSocket.setSoTimeout(0);
                        cbUltimoAck = Integer.parseInt(retorno.trim());
                        sem.release();
                    } catch (SocketTimeoutException e){
                        System.out.println("Timeout. Último ACK recebido: " + cbUltimoAck);
                        sem.release();
                        //reEnviaPct(ultimoAck);
                    }
			}
		} catch (IOException e) {
			System.out.println("Excecao: " + e.getMessage());
		}	break;
	//TODO timer
		default:
		break;
	}
                
    }
}
