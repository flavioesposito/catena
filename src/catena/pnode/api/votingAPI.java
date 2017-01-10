/**
 * @copyright 2017 Computer Science Department laboratory, Saint Louis University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose. 
 */
package catena.pnode.api;

import catena.pnode.util.BidStructure;
import catena.pnode.util.votingData;
import catena.slicespec.impl.googleprotobuf.SliceSpec.Slice;

/**
 * @author flavioesposito
 *
 */
public interface votingAPI {

	/**
	 * 
	 * @param sliceRequested
	 * @param votingData
	 * @return updated voting Data structure
	 */
	votingData nodevoting(Slice sliceRequested, votingData votingData);
	/**
	 * 
	 * @param sliceRequested
	 * @return true if voting is required
	 */
	boolean votingIsNeeded(Slice sliceRequested);
	/**
	 * 
	 * @param sliceRequested
	 * @return bid structure
	 */
	BidStructure computeEta(Slice sliceRequested);
	/**
	 * 
	 * @param sliceRequested
	 * @return true if there was a voting 
	 */
	boolean overbidAttempt(Slice sliceRequested);
}
