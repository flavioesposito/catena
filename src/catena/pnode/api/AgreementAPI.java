/**
 * @copyright 2017 Computer Science Department laboratory, Saint Louis University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose. 
 */
package catena.pnode.api;

import catena.pnode.util.votingData;

/**
 * Agreement phase API
 * @author Flavio Esposito
 *
 */
public interface AgreementAPI {
	/**
	 * 
	 * @param _currentvotingData
	 * @param sliceID
	 */
	void updateVectors(votingData _currentvotingData, int sliceID);
	/**
	 * 
	 * @param vnodeID
	 */
	void updateTimeAndRebroadcast(int vnodeID);
	/**
	 * 
	 * @param vnodeID
	 */
	void resetAndRebroadcastStar(int vnodeID);
	/**
	 * action to take: leave and rebroadcast
	 */
	void leaveAndRebroadcast();
	/**
	 * makes sure physical node does not update and do not rebroadcast
	 */
	void leaveAndNoBroadcast();
	/**
	 * 
	 * @param vnodeID
	 * @param winner
	 */
	void updateAndRebroadcast(int vnodeID, String winner);
	/**
	 * rebroadcast
	 */
	void rebroadcast() ;
	/**
	 * release node subsequent to an outbid
	 * @param vnodeID
	 */
	void releaseSubsequentVnodes(int vnodeID);

}
