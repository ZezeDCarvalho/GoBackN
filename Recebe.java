/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execucao;

import java.util.logging.Level;
import java.util.logging.Logger;
import recepcao.RecebeDados;

/**
 *
 * @author flavio
 */
public class Recebe {
	public static void main(String[] args) {
		RecebeDados r = new RecebeDados();
		r.start();
		
		try {
			r.join();
		} catch (InterruptedException ex) {
			Logger.getLogger(Recebe.class.getName()).log(Level.SEVERE, null, ex);
		}

	}
}
