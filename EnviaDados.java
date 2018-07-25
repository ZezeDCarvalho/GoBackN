package envio;

/**
 * @author flavio, zeze, macb
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
import java.util.Arrays;

public class EnviaDados extends Thread {

    private final int portaLocalEnvio = 2000;
    private final int portaDestino = 2001;
    private final int portaLocalRecebimento = 2003;
    private final String funcao;
    
    // constantes do projeto.
    private static final int tamanhoPacote = 350;
    private static final int tamanhoJanela = 3;
    private final int tempoTimeout = 1000;

    // variáveis do projeto
    private int cabecalho = 0;
    private int cbUltimoAck = 0;
    
    Semaphore sem;
    private static int[][] janelaEnvio = new int[tamanhoJanela][tamanhoPacote];


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
    
    

    @Override
    public void run() {

        switch (this.getFuncao()) {
            case "envia":
                //janelaEnvio = new int[4][tamanhoPacote];
                //variavel onde os dados lidos serao gravados
                int[] dados = new int[tamanhoPacote];
                //contador, para gerar pacotes com 1400 Bytes de tamanho
                //como cada int ocupa 4 Bytes, estamos lendo blocos com tamanhoPacote
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
                        if (cont == tamanhoPacote) {
                            //envia pacotes a cada tamanhoPacote int's lidos.
                            //ou seja, 1400 Bytes.
                            // System.out.println("Sequência: "+this.cabecalho);

                            while (!salvaPct(dados)) {
                                try {
                                this.sleep(100);
                                } catch(InterruptedException e) 
                                {}
                            }
                            enviaPct(dados);
                            cont = 0;
                        }
                    }

                    //ultimo pacote eh preenchido com
                    //-1 ate o fim, indicando que acabou
                    //o envio dos dados.
                    for (int i = cont; i < tamanhoPacote; i++) {
                        dados[i] = -1;
                    }

                  //System.out.println("Sequência: "+cabecalho);
                    while (!salvaPct(dados)) {
                            try {
                            this.sleep(100);
                            } catch(InterruptedException e) 
                            {}
                        }
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
			while (cbUltimoAck != -999) {
                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            try {
                                serverSocket.receive(receivePacket);
                                retorno = new String(receivePacket.getData());
                                System.out.println("Ack recebido "+ retorno +".");
                                //serverSocket.setSoTimeout(0);
                                cbUltimoAck = Integer.parseInt(retorno.trim());
                                apagaPct(cbUltimoAck);
                                sem.release();
                            } catch (SocketTimeoutException e){

                                System.out.println("Timeout. Último ACK recebido: " + cbUltimoAck);
                                reEnviaPct(cbUltimoAck);
                                sem.release();

                            } 
			}
                        serverSocket.setSoTimeout(0);
		} catch (IOException e) {
			System.out.println("Excecao: " + e.getMessage());
		}	break;
	//TODO timer
		default:
		break;
	}
                
    }
    
    private void reEnviaPct(int idUltimoAck) {
        synchronized(this) {
        for (int[] i : janelaEnvio) {
            if (i[0] == idUltimoAck+1) {
                System.out.println("Tentando reenviar pacote: " + (idUltimoAck+1) );
                enviaPct(i);
                return;
                }
            }
        }
    }
    
    private void apagaPct(int ackConfirmado) {
        int[] dados = new int[tamanhoPacote];
        synchronized(this) {
        for (int i = 0; i < janelaEnvio.length; i++) {
            //System.out.println(Arrays.toString(janelaEnvio[i]) );
            //System.out.println("Janela 0 = " + janelaEnvio[i][0] );
            if (janelaEnvio[i][0] == ackConfirmado) {
                System.out.println("Dados do pacote " + janelaEnvio[i][0] + " removidos da janela: " + i);
                janelaEnvio[i] = dados;

                }
            }
        }
    }
    
    private boolean salvaPct(int[] dados) {
        dados = Arrays.copyOf(dados, dados.length);
        //System.out.println(Arrays.toString(dados) );
        //System.out.println(dados[0] );
        synchronized(this) {
        for (int i = 0; i < janelaEnvio.length; i++) {
            if (janelaEnvio[i][0] == 0) {
                janelaEnvio[i] = dados;
                System.out.println("Dados do pacote " + janelaEnvio[i][0] + " salvos na janela: " + i);
                return true;
                }
            }
        System.out.println("Janela de envio cheia.");
        return false;
        }
    }
    
    
}
