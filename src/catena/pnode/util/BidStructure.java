
package catena.pnode.util;

/**
 * Vote data structure
 * @author flavioesposito
 * @version 1.0
 */
public class VoteStructure {
	
	private double Uij = Double.MIN_VALUE;
	private int eta = -1;
	
	public VoteStructure() {}
	
	public VoteStructure(double Uij, int eta) {
		this.Uij = Uij;
		this.eta = eta;
	}

	/**
	 * @return the uij
	 */
	public double getUij() {
		return Uij;
	}

	/**
	 * @param uij the uij to set
	 */
	public void setUij(double uij) {
		Uij = uij;
	}

	/**
	 * @return the eta
	 */
	public int getEta() {
		return eta;
	}

	/**
	 * @param eta the eta to set
	 */
	public void setEta(int eta) {
		this.eta = eta;
	}
	
}
