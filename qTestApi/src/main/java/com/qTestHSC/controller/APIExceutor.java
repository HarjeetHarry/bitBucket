package com.qTestHSC.controller;

import java.util.Arrays;
import java.util.List;

import com.qTestHSC.core.Constants;
import com.qTestHSC.core.TCDetails;
import com.qTestHSC.gateway.QTestComponent;
import com.qTestHSC.util.Utility;

public class APIExceutor {

	static String QTest_Synchronizer_PATH = System.getProperty(Constants.UserDir) + Constants.QTest_Synchronizer_Path;	
	public static void main(String[] args) {	

		List<TCDetails> listTCDetails = Utility.loadInputDataSheet(QTest_Synchronizer_PATH, Constants.TestExecution);
		QTestComponent.StatusChangeWithAPI(listTCDetails);  
		QTestComponent.syncQTestProp(listTCDetails);
		QTestComponent.getQTestSuiteData();

		Utility.processAutomationDatafromExcel();


	}

}

