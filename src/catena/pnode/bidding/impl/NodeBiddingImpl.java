/**
 * @copyright 2013 Computer Science Department laboratory, Boston University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose. 
 */
package catena.pnode.voting.impl;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import rina.rib.impl.RIBImpl;
import catena.pnode.api.votingAPI;
import catena.pnode.util.BidStructure;
import catena.pnode.util.votingData;
import catena.pnode.util.PnodeUtil;
import catena.slicespec.impl.googleprotobuf.SliceSpec.Slice;
import catena.slicespec.impl.googleprotobuf.SliceSpec.Slice.vNode;


/**
 * @author Flavio Esposito
 * implementation of the node voting class that every ph.node performs 
 */
public class NodevotingImpl implements votingAPI {
	/**
	 * pnode name
	 */
	private String _pNodeName = null;
	/**
	 * Resource Information Base that contains all info
	 */
	RIBImpl rib = null;
	/**
	 * name of the bidder 
	 */
	String pNodeBidder = null;
	
	
	/**
	 * allocation policy default MAD (SAD or MAD or write your own)
	 */
	private String _allocationPolicy= null;
	/**
	 * allocation vector for each slice hosted or in hosting attempt <sliceID, <vnode, _pNodeNameOrAddress>>
	 */
	private LinkedHashMap<Integer, LinkedHashMap<Integer,String>> _allocationVectorMap = null;

	/**
	 * bid vector for each slice hosted or in hosting attempt
	 */
	private LinkedHashMap<Integer, LinkedList<Double>> _bidVectorMap = null;

	/**
	 * time stamp map
	 */
	private LinkedHashMap<Integer, LinkedHashMap<Integer, Long>> _votingTimeMap = null;
	
	/**
	 * bundle vector for each slice hosted or in hosting attempt
	 */
	private LinkedHashMap<Integer, LinkedList<Integer>> _mMap = null;

	/**
	 * service providers can send requests for embedding
	 */
	private String _mySP = null;

	/**
	 * to reset to this value when the bundle is reset
	 */
	private double _nodeStressBeforeThisSliceRequest = 0.01;

	/**
	 * global node capacity
	 */
	private double _nodeStress = 0.01;

	/**
	 * initial stress on physical node
	 */
	private double _stress = 0.01;

	/**
	 * target node capacity
	 */
	private double _targetNodeCapacity = 100.0;


	/**
	 * target adjacent link capacity
	 */
	private double _targetLinkCapacity = 500.0;

	/**
	 * target stress
	 */
	private double _targetStress = 1.0;


	/**
	 * <sliceID, iteration_t >
	 */
	private LinkedHashMap<Integer, Integer> _iterationMap  =null;
	/**
	 * <sliceID,<pnode id, vnode id>> known so far
	 */
	private LinkedHashMap<Integer,LinkedHashMap<Integer, Integer>> _nodeMappingMap  = null;

	
	/**
	 * node voting utility function
	 */
	private String nodeUtility = null;//cad.utility = utility1

	/**
	 * assignment Vector: least or most informative (x or a)
	 */
	private String assignmentVectorPolicy = null; //# least or most cad.assignmentVector = least
	/**
	 * bidVectorLength
	 */
	private int bidVectorLengthPolicy = 0;//	cad.bidVectorLength = 1 

	/**
	 * pnode utility function
	 */
	private PnodeUtil _NodeUtil= null;
	
	
	/**
	 * constructor
	 */
	public NodevotingImpl(RIBImpl rib, String pNodeBidder){
		this.rib = rib;
		this.pNodeBidder = pNodeBidder;
	}

	/**
	 * Physical Node voting
	 * procedure 1 at 
	 * www.cs.bu.edu/techreports/pdf/2012-014-slice-embedding-with-guarantees.pdf
	 * 
	 * @param sliceRequested
	 */
	public votingData nodevoting(Slice sliceRequested, votingData votingData) {
		
			votingData newvotingData = new votingData();
			initializevotingData(votingData);
			
			

			if(sliceRequested == null) {
				this.rib.RIBlog.warnLog("NodevotingImpl::nodevoting: sliceRequested to: "+this.getpNodeBidder()+ "is null !!! ");
				return votingData;
			}
			int sliceID = sliceRequested.getSliceID();
			increaseRound(sliceID);

			rib.RIBlog.infoLog("CADlog: pNode "+this.getpNodeBidder()+ " voting PHASE started " +
					" \n for sliceName: "+ sliceRequested.getName()+
					" \n slice topology: "+sliceRequested.getTopology()+
					" \n slice id: "+	   sliceRequested.getSliceID());


			List<vNode> requested_vNodeList = sliceRequested.getVirtualnodeList();
			rib.RIBlog.debugLog("requested vNodeList : "+requested_vNodeList );

			//voting phase: 
			//	1: Input: a_i(t-1), b_i(t-1)
			//	2: Output: a_i(t), b_i(t), m_i(t)
			//	3: a_i(t) = a_i(t -1), b_i(t) = b_i(t - 1); 
			//	4: if votingIsNeeded(a_i(t); T_i) then
			boolean bidisNeeded = true;
			rib.RIBlog.debugLog("NodevotingImpl::nodevoting: bidisNeeded BEFORE (should be true): "+bidisNeeded);
			boolean bundleAlreadyReset = false;

			rib.RIBlog.debugLog("NodevotingImpl::nodevoting:sliceRequested.getVirtualnodeCount(): "+sliceRequested.getVirtualnodeCount());

			while(bidisNeeded ) { 
				bidisNeeded = votingIsNeeded(sliceRequested);
				rib.RIBlog.debugLog("NodevotingImpl::nodevoting: bidisNeeded AFTER: "+bidisNeeded);		

				if(bidisNeeded == false){	
					rib.RIBlog.debugLog("NodevotingImpl::nodevoting: bidisNeeded = false");		
					break;
				} else {   	

					LinkedList<Integer> m = _mMap.get(sliceID);
					rib.RIBlog.debugLog("NodevotingImpl::nodevoting: m BEFORE overbidAttempt: "+m);		

					//	5: if exists j : h_{ij} = I(U_ij (t) > b_ij (t)) != 0  then

					boolean existsJ = overbidAttempt(sliceRequested); //this pnode could overbid
					rib.RIBlog.debugLog("NodevotingImpl::nodevoting: existsJ AFTER overbidAttempt : "+existsJ);
					if(existsJ) {
						//m_i(t) = null; the first time you are filling up the bundle
						if(!bundleAlreadyReset){
							m.clear();
							//restore original residual node capacity
							set_nodeStressBeforeThisSliceRequest(get_nodeCapacity());
							bundleAlreadyReset = true;
						}
						//	6:  eta = argmax_{j \in V^H} {h_ij * U_ij}
						BidStructure bidStructure = new BidStructure();
						rib.RIBlog.debugLog("NodevotingImpl::nodevoting: BEFORE voting eta : "+bidStructure.getEta());
						rib.RIBlog.debugLog("NodevotingImpl::nodevoting: BEFORE voting Uij : "+bidStructure.getUij());

						bidStructure = computeEta(sliceRequested);

						rib.RIBlog.debugLog("NodevotingImpl::nodevoting: AFTER voting eta : "+bidStructure.getEta());
						rib.RIBlog.debugLog("NodevotingImpl::nodevoting: AFTER voting Uij : "+bidStructure.getUij());

						//	7: m_i(t) = mi(t).append(eta) // append to bundle	
						if(bidStructure.getEta()!= -1){

							double oldNodeCapacity = this.get_nodeCapacity();
							double newRequestedVnodeCapacity = sliceRequested.getVirtualnode(bidStructure.getEta()).getVNodeCapacity();
							this.set_nodeStressBeforeThisSliceRequest(oldNodeCapacity+newRequestedVnodeCapacity);
							if(this.get_nodeStressBeforeThisSliceRequest() > this.get_targetNodeCapacity())
							{
								rib.RIBlog.errorLog("NodevotingImpl::nodevoting: ERROR node capacity above the allowed limit !!!!");
							}

							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: newRequestedVnodeCapacity = "+sliceRequested.getVirtualnode(bidStructure.getEta()).getVNodeCapacity());
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: get_nodeCapacity()= "+this.get_nodeCapacity());
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: get_nodeStressBeforeThisSliceRequest() = "+this.get_nodeStressBeforeThisSliceRequest());


							//if the bundle does not contain eta already
							if(!m.contains(bidStructure.getEta()))
							{
								m.add(bidStructure.getEta());
								_mMap.remove(sliceID);
								_mMap.put(sliceID, m);
							}
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: m AFTER compute eta: "+m);
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: _mMap AFTER compute eta: "+_mMap);


							//	8: b_i(t) = U_i(t)
							LinkedList<Double> b = this._bidVectorMap.get(sliceID);
							_bidVectorMap.remove(sliceID);

							b.set(bidStructure.getEta(), bidStructure.getUij());
							_bidVectorMap.put(sliceID, b);
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: b AFTER compute eta: "+b);
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: _bidVectorMap AFTER compute eta: "+_bidVectorMap);



							//	9: update(eta; a_i(t))
							LinkedHashMap<Integer,String> a = _allocationVectorMap.get(sliceID);
							_allocationVectorMap.remove(sliceID);
							a.put(bidStructure.getEta(), this.get_pNodeName());
							_allocationVectorMap.put(sliceID, a);
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: a AFTER compute eta: "+a);
							rib.RIBlog.debugLog("NodevotingImpl::nodevoting: _allocationVectorMap AFTER compute eta: "+_allocationVectorMap);
						}

					} //end if

				}//end else 


			}//end while
			
			//set voting time for all elements in the bundle
			

			
			newvotingData = updatevotingData(newvotingData);
			
			rib.RIBlog.debugLog("NodevotingImpl::nodevoting: get_allocationPolicy: "+newvotingData.get_allocationPolicy());
			rib.RIBlog.debugLog("NodevotingImpl::nodevoting: _allocationVectorMap: "+newvotingData.get_allocationVectorMap());
			
			rib.RIBlog.debugLog("NodevotingImpl::nodevoting: _bidVectorMap: "+newvotingData.get_bidVectorMap());
			rib.RIBlog.debugLog("NodevotingImpl::nodevoting: _mMap: "+newvotingData.get_mMap());

			
			
			return newvotingData;
		}

	
	
	
	/**
	 * create structure for nodevoting
	 * @return votingData
	 */
	private votingData updatevotingData(votingData votingData) {

		votingData.set_pNodeName(_pNodeName);
		votingData.set_allocationPolicy(_allocationPolicy);
		votingData.set_allocationVectorMap(_allocationVectorMap);
		votingData.set_bidVectorMap(_bidVectorMap);
		
		votingData.set_votingTimeMap(_votingTimeMap);
		
		votingData.set_iterationMap(_iterationMap);
		votingData.set_mMap(_mMap);
		votingData.set_mySP(_mySP);
		votingData.set_nodeMappingMap(_nodeMappingMap);
		votingData.set_nodeStress(_nodeStress);
		votingData.set_nodeStressBeforeThisSliceRequest(_nodeStressBeforeThisSliceRequest);
		votingData.set_NodeUtil(_NodeUtil);
		votingData.set_stress(_stress);
		votingData.set_targetLinkCapacity(_targetLinkCapacity);
		votingData.set_targetNodeCapacity(_targetNodeCapacity);
		votingData.set_targetStress(_targetStress);

		return votingData;
	}

	/**
	 * initialize voting Data for voting phase
	 * @param votingData
	 */
	private void initializevotingData(votingData votingData) {
		
		_pNodeName = votingData.get_pNodeName();
		_allocationPolicy = votingData.get_allocationPolicy();
		_allocationVectorMap = votingData.get_allocationVectorMap();
		_bidVectorMap = votingData.get_bidVectorMap();
		_iterationMap = votingData.get_iterationMap();
		
		_votingTimeMap = votingData.get_votingTimeMap();
		

		_mMap = votingData.get_mMap();
		_mySP = votingData.get_mySP();
		_nodeMappingMap = votingData.get_nodeMappingMap();
		_nodeStress = votingData.get_nodeStress();
		_nodeStressBeforeThisSliceRequest = votingData.get_nodeStressBeforeThisSliceRequest();
		_NodeUtil = votingData.get_NodeUtil();
		_stress = votingData.get_nodeStress();
		_targetLinkCapacity = votingData.get_targetLinkCapacity();
		_targetNodeCapacity = votingData.get_targetNodeCapacity();
		_targetStress = votingData.get_targetStress();
		
	}

	/**
	 * 
	 * @param sliceRequested
	 * @return true if it is possible to overbid, i.e. 
	 * if exists j : h_{ij} = I(U_ij (t) > b_ij (t)) != 0 
	 */
	public boolean overbidAttempt(Slice sliceRequested) {

		rib.RIBlog.debugLog("NodevotingImpl::overbidAttempt: START ======================");

		List<vNode> requested_vNodeList = sliceRequested.getVirtualnodeList();
		for (int j=0; j <  requested_vNodeList.size(); j++) {

			double vnodeCapacity = requested_vNodeList.get(j).getVNodeCapacity();
			double hij = _NodeUtil.computeUtility(vnodeCapacity, this.get_targetNodeCapacity(), this.get_nodeStressBeforeThisSliceRequest());

			rib.RIBlog.debugLog("NodevotingImpl::overbidAttempt: trying to bid on vnode with id: "+j);
			rib.RIBlog.debugLog("NodevotingImpl::overbidAttempt: b_ij: "+_bidVectorMap.get(sliceRequested.getSliceID()).get(j));
			rib.RIBlog.debugLog("hij: "+ hij);

			if( hij > _bidVectorMap.get(sliceRequested.getSliceID()).get(j)) {
				rib.RIBlog.debugLog("NodevotingImpl::overbidAttempt END: bid is possible ======================");
				return true;
			}
		}
		rib.RIBlog.debugLog("NodevotingImpl::overbidAttempt END: bid is NOT possible ===*****===============");
		return false;
	}


	
	/**
	 * increase the round t for logging purposes when the voting is needed  
	 * @param sliceID
	 */
	private void increaseRound(int sliceID) {

		int oldRound = -1;
		if(this._iterationMap.containsKey(sliceID)) 
		{
			oldRound = _iterationMap.get(sliceID);
			this._iterationMap.put(sliceID, oldRound+1);
		}else
			this._iterationMap.put(sliceID, 1);
		rib.RIBlog.infoLog("Iteration t = "+_iterationMap.get(sliceID)+" for slice: "+sliceID);


	}
	
	

	/**
	 * check if voting is needed, because there is enough capacity 
	 * or because the auction policy allows another voting, 
	 * and eventually orders voting phase termination
	 * @return true if voting is needed 
	 */
	public boolean votingIsNeeded(Slice sliceRequested) {

		int sliceID = sliceRequested.getSliceID();

		rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: _bidVectorMap.get(sliceID).size(): "+_bidVectorMap.get(sliceID).size());
		rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: get_allocationPolicy(): "+get_allocationPolicy());		
		rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: sliceRequested.getVirtualnodeList().size(): "+sliceRequested.getVirtualnodeList().size());
		rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: _bidVectorMap.get(sliceID).size(): "+_bidVectorMap.get(sliceID).size());		

		//if there is already a bid and the policy is SAD return false
		LinkedList<Double> b = _bidVectorMap.get(sliceID);
		Iterator<Double> bIter = b.iterator();
		int currentBids = 0;
		while(bIter.hasNext()){
			double bElement = bIter.next();
			if (bElement >0)
				currentBids++; 
		}
		rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: currentBids: " +currentBids);
		rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: sliceRequested.getVirtualnodeList().size(): " +sliceRequested.getVirtualnodeList().size());

		if(currentBids > 0 && get_allocationPolicy().equals("SAD") 
				|| sliceRequested.getVirtualnodeList().size() == currentBids  ) 
			return false;

		//TODO: consider implement other heuristics with different stress (e.g., also considering links) 
		// if there is not enough stress return false 
		List<vNode> requested_vNodeList = sliceRequested.getVirtualnodeList();
		for (vNode vnode: requested_vNodeList) {
			int tempCapacity = vnode.getVNodeCapacity();

			rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: vnode: "+vnode + "tempCapacity: "+vnode.getVNodeCapacity());
			rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: get_nodeStressBeforeThisSliceRequest(): "+get_nodeStressBeforeThisSliceRequest());		
			rib.RIBlog.debugLog("NodevotingImpl::votingIsNeeded: get_targetNodeCapacity: "+get_targetNodeCapacity());		

			if(tempCapacity + get_nodeStressBeforeThisSliceRequest() <= get_targetNodeCapacity() )
				return true;
		}
		//voting on any virtual node would overcome the limit on the physical capacity of this node 
		return false;
	}


	/**
	 * 	compute eta = argmax_{j \in V^H} {h_ij * U_ij}
	 * @param sliceRequested
	 * @return eta: ID of the vnode with maximum reward for this pnode
	 */
	public BidStructure computeEta(Slice sliceRequested) {

		rib.RIBlog.debugLog("NodevotingImpl::computeEta: START ====================");
		int sliceID = sliceRequested.getSliceID();
		int eta = -1;
		BidStructure bidStructure = new BidStructure();

		// nodes already in the bundles: we don't bid on those again
		LinkedList<Integer> vNodesToExclude = new LinkedList<Integer>();

		List<vNode> requested_vNodeList = sliceRequested.getVirtualnodeList();

		LinkedList<Double> b = _bidVectorMap.get(sliceID); 
		LinkedList<Integer> m = _mMap.get(sliceID);

		rib.RIBlog.debugLog("NodevotingImpl::computeEta: m: "+m);
		rib.RIBlog.debugLog("NodevotingImpl::computeEta: m.size: "+m.size());
		rib.RIBlog.debugLog("NodevotingImpl::computeEta: b: "+b);
		rib.RIBlog.debugLog("NodevotingImpl::computeEta: b.size: "+b.size());



		//exclude vnode IDs that are already in the bundle 
		Iterator<Integer> mIter = m.iterator();

		while(mIter.hasNext()) {
			int vnodeInBundle = mIter.next(); 
			rib.RIBlog.debugLog("NodevotingImpl::computeEta: vnodeInBundle: "+vnodeInBundle);
			vNodesToExclude.add(m.get(vnodeInBundle));
			rib.RIBlog.debugLog("NodevotingImpl::computeEta: vNodesToExclude: "+vNodesToExclude);

		}
		rib.RIBlog.debugLog("NodevotingImpl::computeEta: FINAL vNodesToExclude: "+vNodesToExclude);


		double Uij = Double.MIN_VALUE;
		double temp_Uij = Double.MIN_VALUE;
		for (int j=0; j <  requested_vNodeList.size(); j++) {

			//a vnode can be already in the bundle
			if(vNodesToExclude.contains(j))
				continue;

			double vnodeCapacity = requested_vNodeList.get(j).getVNodeCapacity();
			temp_Uij = _NodeUtil.computeUtility(vnodeCapacity, m, sliceRequested, this.get_targetNodeCapacity(), this.get_nodeStressBeforeThisSliceRequest());
			rib.RIBlog.debugLog("NodevotingImpl::computeEta: temp_Uij: "+temp_Uij);

			//argmax_{j \in V^H} {h_ij * U_ij}
			if( temp_Uij > Uij){
				eta = j;
				Uij = temp_Uij;
			}

		}
		bidStructure.setEta(eta);
		bidStructure.setUij(Uij);


		rib.RIBlog.infoLog("NodevotingImpl::computeEta: "+this.get_pNodeName()+ " eta: "+eta+" for sliceID: "+sliceRequested.getSliceID());
		rib.RIBlog.infoLog("NodevotingImpl::computeEta: "+this.get_pNodeName()+ " Uij: "+Uij+" for sliceID: "+sliceRequested.getSliceID());

		rib.RIBlog.debugLog("NodevotingImpl::computeEta: END ====================");
		return bidStructure;
	}


		

	
	

	
	/**
	 * @return the rib
	 */
	public RIBImpl getRib() {
		return rib;
	}

	/**
	 * @param rib the rib to set
	 */
	public void setRib(RIBImpl rib) {
		this.rib = rib;
	}

	/**
	 * @return the pNodeBidder
	 */
	public String getpNodeBidder() {
		return pNodeBidder;
	}

	/**
	 * @param pNodeBidder the pNodeBidder to set
	 */
	public void setpNodeBidder(String pNodeBidder) {
		this.pNodeBidder = pNodeBidder;
	}

	/**
	 * @return the _allocationPolicy
	 */
	public String get_allocationPolicy() {
		return _allocationPolicy;
	}

	/**
	 * @param _allocationPolicy the _allocationPolicy to set
	 */
	public void set_allocationPolicy(String _allocationPolicy) {
		this._allocationPolicy = _allocationPolicy;
	}

	/**
	 * @return the _allocationVectorMap
	 */
	public LinkedHashMap<Integer, LinkedHashMap<Integer, String>> get_allocationVectorMap() {
		return _allocationVectorMap;
	}

	/**
	 * @param _allocationVectorMap the _allocationVectorMap to set
	 */
	public void set_allocationVectorMap(
			LinkedHashMap<Integer, LinkedHashMap<Integer, String>> _allocationVectorMap) {
		this._allocationVectorMap = _allocationVectorMap;
	}

	/**
	 * @return the _bidVectorMap
	 */
	public LinkedHashMap<Integer, LinkedList<Double>> get_bidVectorMap() {
		return _bidVectorMap;
	}

	/**
	 * @param _bidVectorMap the _bidVectorMap to set
	 */
	public void set_bidVectorMap(
			LinkedHashMap<Integer, LinkedList<Double>> _bidVectorMap) {
		this._bidVectorMap = _bidVectorMap;
	}

	/**
	 * @return the _mMap
	 */
	public LinkedHashMap<Integer, LinkedList<Integer>> get_mMap() {
		return _mMap;
	}

	/**
	 * @param _mMap the _mMap to set
	 */
	public void set_mMap(LinkedHashMap<Integer, LinkedList<Integer>> _mMap) {
		this._mMap = _mMap;
	}

	/**
	 * @return the _mySP
	 */
	public String get_mySP() {
		return _mySP;
	}

	/**
	 * @param _mySP the _mySP to set
	 */
	public void set_mySP(String _mySP) {
		this._mySP = _mySP;
	}

	/**
	 * @return the _nodeStressBeforeThisSliceRequest
	 */
	public double get_nodeStressBeforeThisSliceRequest() {
		return _nodeStressBeforeThisSliceRequest;
	}

	/**
	 * @param _nodeStressBeforeThisSliceRequest the _nodeStressBeforeThisSliceRequest to set
	 */
	public void set_nodeStressBeforeThisSliceRequest(
			double _nodeStressBeforeThisSliceRequest) {
		this._nodeStressBeforeThisSliceRequest = _nodeStressBeforeThisSliceRequest;
	}

	/**
	 * @return the _nodeCapacity
	 */
	public double get_nodeCapacity() {
		return _nodeStress;
	}


	/**
	 * @param _nodeStress to set before sending the message out
	 */
	public void set_nodeCapacity(double _nodeStress) {
		this._nodeStress = _nodeStress;
	}

	/**
	 * @return the _stress
	 */
	public double get_stress() {
		return _stress;
	}

	/**
	 * @param _stress the _stress to set
	 */
	public void set_stress(double _stress) {
		this._stress = _stress;
	}

	/**
	 * @return the _targetNodeCapacity
	 */
	public double get_targetNodeCapacity() {
		return _targetNodeCapacity;
	}

	/**
	 * @param _targetNodeCapacity the _targetNodeCapacity to set
	 */
	public void set_targetNodeCapacity(double _targetNodeCapacity) {
		this._targetNodeCapacity = _targetNodeCapacity;
	}

	/**
	 * @return the _targetLinkCapacity
	 */
	public double get_targetLinkCapacity() {
		return _targetLinkCapacity;
	}

	/**
	 * @param _targetLinkCapacity the _targetLinkCapacity to set
	 */
	public void set_targetLinkCapacity(double _targetLinkCapacity) {
		this._targetLinkCapacity = _targetLinkCapacity;
	}

	/**
	 * @return the _targetStress
	 */
	public double get_targetStress() {
		return _targetStress;
	}

	/**
	 * @param _targetStress the _targetStress to set
	 */
	public void set_targetStress(double _targetStress) {
		this._targetStress = _targetStress;
	}

	/**
	 * @return the _iterationMap
	 */
	public LinkedHashMap<Integer, Integer> get_iterationMap() {
		return _iterationMap;
	}

	/**
	 * @param _iterationMap the _iterationMap to set
	 */
	public void set_iterationMap(LinkedHashMap<Integer, Integer> _iterationMap) {
		this._iterationMap = _iterationMap;
	}

	/**
	 * @return the _nodeMappingMap
	 */
	public LinkedHashMap<Integer, LinkedHashMap<Integer, Integer>> get_nodeMappingMap() {
		return _nodeMappingMap;
	}

	/**
	 * @param _nodeMappingMap the _nodeMappingMap to set
	 */
	public void set_nodeMappingMap(
			LinkedHashMap<Integer, LinkedHashMap<Integer, Integer>> _nodeMappingMap) {
		this._nodeMappingMap = _nodeMappingMap;
	}

	/**
	 * @return the nodeUtility
	 */
	public String getNodeUtility() {
		return nodeUtility;
	}

	/**
	 * @param nodeUtility the nodeUtility to set
	 */
	public void setNodeUtility(String nodeUtility) {
		this.nodeUtility = nodeUtility;
	}

	/**
	 * @return the assignmentVectorPolicy
	 */
	public String getAssignmentVectorPolicy() {
		return assignmentVectorPolicy;
	}

	/**
	 * @param assignmentVectorPolicy the assignmentVectorPolicy to set
	 */
	public void setAssignmentVectorPolicy(String assignmentVectorPolicy) {
		this.assignmentVectorPolicy = assignmentVectorPolicy;
	}

	/**
	 * @return the bidVectorLengthPolicy
	 */
	public int getBidVectorLengthPolicy() {
		return bidVectorLengthPolicy;
	}

	/**
	 * @param bidVectorLengthPolicy the bidVectorLengthPolicy to set
	 */
	public void setBidVectorLengthPolicy(int bidVectorLengthPolicy) {
		this.bidVectorLengthPolicy = bidVectorLengthPolicy;
	}

	/**
	 * @return the _NodeUtil
	 */
	public PnodeUtil get_NodeUtil() {
		return _NodeUtil;
	}

	/**
	 * @param _NodeUtil the _NodeUtil to set
	 */
	public void set_NodeUtil(PnodeUtil _NodeUtil) {
		this._NodeUtil = _NodeUtil;
	}

	/**
	 * @return the _pNodeName
	 */
	public String get_pNodeName() {
		return _pNodeName;
	}

	/**
	 * @param _pNodeName the _pNodeName to set
	 */
	public void set_pNodeName(String _pNodeName) {
		this._pNodeName = _pNodeName;
	}
	
	/**
	 * @return the _mMap
	 */
	public LinkedList<Integer> get_m(int sliceID) {
		return _mMap.get(sliceID);
	}

	/**
	 * the bundle vector to set
	 * @param sliceID
	 * @param m
	 */
	public void set_m(int sliceID, LinkedList<Integer> m) {
		this._mMap.put(sliceID, m);
	}
}
