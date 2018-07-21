/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execucao;

import envio.EnviaDados;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author flavio
 */
public class Envia {
	public static void main(String[] args) {
		Semaphore sem = new Semaphore(1);
		
		EnviaDados e = new EnviaDados(sem, "envia");
		EnviaDados r = new EnviaDados(sem, "ack");
		
		r.start();
		e.start();
		
		try {
			e.join();
			System.out.println("fim envia");
			r.join();
			System.out.println("fim recebe Ack");
		} catch (InterruptedException ex) {
			Logger.getLogger(Envia.class.getName()).log(Level.SEVERE, null, ex);
		}

	}
}
