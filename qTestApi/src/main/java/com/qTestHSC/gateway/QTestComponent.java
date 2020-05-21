package com.qTestHSC.gateway;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import io.restassured.path.json.JsonPath;

import com.jayway.jsonpath.DocumentContext;
import com.qTestHSC.core.Constants;
import com.qTestHSC.core.TCDetails;
import com.qTestHSC.util.Utility;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class QTestComponent {


	/**
	 * This method returns map of all Id's from Test suite API
	 * @return
	 */
	static String QTest_Synchronizer_PATH = System.getProperty(Constants.UserDir) + Constants.QTest_Synchronizer_Path;
	static Logger log = Logger.getLogger(QTestComponent.class);

	public static Map<String , Map<String, List<Integer>>> getQTestSuiteData()  {
		String newURI = Utility.getProperty(Constants.Url) + Utility.getProperty(Constants.ProjectId) + Utility.getProperty(Constants.ContextPath);	
		List<String> allTestSuitesId = Utility.getAllInputByHeader(QTest_Synchronizer_PATH, Constants.TestExecution, Constants.header_TestSuiteParentId).get(Constants.header_TestSuiteParentId);	
		String testSuiteId = allTestSuitesId.get(0).trim();   

		Map<String , Map<String, List<Integer>>> testSuiteData= new LinkedHashMap<String, Map<String, List<Integer>>>();
		// Retrieving test suite data for each test Suite Id
		for(int i=0; i< allTestSuitesId.size(); i++) {
			if(testSuiteData.containsKey(allTestSuitesId.get(i))) {
				continue;
			}

			int parentIdValue = Integer.parseInt(testSuiteId);		
			RequestSpecification requestSpecification= RestAssured.given().
					queryParam(Constants.ParentId, parentIdValue).queryParam(Constants.ParentType, Constants.ParentTypeValue).
					headers(Constants.Authorization , Utility.getProperty(Constants.Token));
			requestSpecification.contentType(ContentType.JSON);
			Response response = requestSpecification.get(newURI);		
			System.out.println(response.getStatusCode());
			JsonPath testRunId= new JsonPath(response.asString());
			List<Integer> listTestRun = testRunId.getList(Constants.TestRunId);
			List<Integer> listTestRunParentId= testRunId.getList(Constants.TestRunParentId);       
			List<Integer> testLogIdlist= new ArrayList<Integer>();

			// Retrieving TestRunId, TestRunParentId, TestLogId for Test Suite 
			for(int j=0; j<listTestRun.size(); j++) {
				int trId = listTestRun.get(j); 				
				RequestSpecification requestSpec= RestAssured.given().
						pathParam(Constants.TestRunsIdPathParam, trId).headers(Constants.Authorization , Utility.getProperty(Constants.Token));
				requestSpec.contentType(ContentType.JSON);
				Response res = requestSpec.get(Utility.getProperty(Constants.Url) +  Utility.getProperty(Constants.ProjectId) + Utility.getProperty(Constants.ContextPathTestRun));
				DocumentContext jsonCon = com.jayway.jsonpath.JsonPath.parse(res.asString());
				Integer listofTLIds = jsonCon.read(Constants.TestLogJSON);
				testLogIdlist.add(listofTLIds);	   
			}

			Map<String, List<Integer>> mapId= new LinkedHashMap<String, List<Integer>>();
			mapId.put(Constants.TestRunIdMapKey, listTestRun); 
			mapId.put(Constants.TestRunPidMapKey, listTestRunParentId);               
			mapId.put(Constants.TestLogIdMapKey, testLogIdlist);  		

			testSuiteData.put(allTestSuitesId.get(i), mapId);

			System.out.println(testSuiteData);
		}
		return testSuiteData;

	}


	/**
	 * This method giving API status code which required to make JSON payload for PUT call
	 * @return
	 */

	public static HashMap<String, String> apiStatusCode()  {
		HashMap<String, String> statusMap = new LinkedHashMap<String, String>(); 
		statusMap.put(Constants.Pass, Constants.PassCode);
		statusMap.put(Constants.Fail, Constants.FailCode);
		statusMap.put(Constants.Incomplete, Constants.IncompleteCode);
		statusMap.put(Constants.Block, Constants.BlockCode);
		statusMap.put( Constants.Unexecuted, Constants.UnexecutedCode);
		statusMap.put(Constants.NeedRetested,Constants.NeedRetestedCode);
		statusMap.put(Constants.PWE,Constants.PWEcode);
		statusMap.put(Constants.NotPlanned, Constants.NotPlannedCode);
		return statusMap;

	}



	/** This method will change test case status & write qTest output to the excel
	 * @SuppressWarnings("unlikely-arg-type")
	 * @param listTCDetails
	 */

	@SuppressWarnings("unlikely-arg-type")
	public static void StatusChangeWithAPI(List<TCDetails> listTCDetails) { 

		Map<String, Map<String, List<Integer>>> testSuiteDataMap = QTestComponent.getQTestSuiteData();
		for(int j=0; j<listTCDetails.size(); j++ ) {
			TCDetails tc =new TCDetails();
			tc= listTCDetails.get(j);

			Map<String, List<Integer>> idsList =testSuiteDataMap.get(tc.getTestSuiteParentId());
			List<Integer> listRunIds = idsList.get(Constants.TestRunIdMapKey);
			List<Integer> listLogIds = idsList.get(Constants.TestLogIdMapKey);
			List<Integer> listRunPids = idsList.get(Constants.TestRunPidMapKey);
			System.out.println("Print TR id: " + listRunPids.contains(tc.getTestRunId()));

			if(listRunPids.contains(tc.getTestRunId()) && tc.getAutExecutionStatus() != null && tc.getAutExecutionStatus() !="" && !("Success".equalsIgnoreCase(tc.getSyncQTestStatus()))) {
				int idIndex = listRunPids.indexOf(tc.getTestRunId());
				System.out.println(idIndex);		

				SimpleDateFormat formatter = new SimpleDateFormat(Constants.JsonTimeStampFormat);
				formatter.setTimeZone(TimeZone.getTimeZone(Constants.TimeZone));	

				JSONObject	strJSON = new JSONObject();
				JSONObject	strJSON2 = new JSONObject();		
				strJSON.put(Constants.ExeStartDate, formatter.format(new Date()) + Constants.TimeAddOn);
				strJSON.put(Constants.ExeEndDate, formatter.format(new Date()) + Constants.TimeAddOn);

				strJSON2.put(Constants.TestRunId, apiStatusCode().get(tc.getAutExecutionStatus()));
				strJSON.put(Constants.Status, strJSON2);

				RequestSpecification requestSpecification= RestAssured.given().
						pathParam(Constants.Test_RunId, listRunIds.get(idIndex)).pathParam(Constants.Test_LogId, listLogIds.get(idIndex)).
						headers(Constants.Authorization , Utility.getProperty(Constants.Token)).body(strJSON.toString());
				requestSpecification.contentType(ContentType.JSON);
				Response response = requestSpecification.put(Utility.getProperty(Constants.Url) +  Utility.getProperty(Constants.ProjectId) + Utility.getProperty(Constants.CONTEXT_PATH_TR_TL));	
				log.info("Status Code: "+response.getStatusCode());


				Format f = new SimpleDateFormat(Constants.Output_TimeStamp);
				String timeStamp = f.format(new Date());

				Map<String, String> qTestOutput = new LinkedHashMap<String, String>();
				qTestOutput.put(Constants.header_TimeStamp, timeStamp);

				if(response.getStatusCode()==200) {
					qTestOutput.put(Constants.header_qTestStatus, "Success");						
				}else {
					qTestOutput.put(Constants.header_qTestStatus, "Failure");
					qTestOutput.put(Constants.header_qTestFailureReason, response.getStatusLine());
				}

				Utility.writeOutputToExcel(QTest_Synchronizer_PATH, Constants.TestExecution, tc, qTestOutput );
				System.out.println("Success data writing" + qTestOutput );				
			}  		
		} 

	}

	/**
	 * Method update the FaliureReason/Test Output/Comments in qTest
	 * @param listTCDetails
	 */

	@SuppressWarnings("unlikely-arg-type")
	public static void syncQTestProp(List<TCDetails> listTCDetails) {
		List<String> failureReasonList = Utility.getAllInputByHeader(QTest_Synchronizer_PATH, Constants.TestExecution, Constants.header_FailureReason).get(Constants.header_FailureReason);
		List<String> testOutputList = Utility.getAllInputByHeader(QTest_Synchronizer_PATH, Constants.TestExecution, Constants.header_TestOutput).get(Constants.header_TestOutput);
		List<String> commentList = Utility.getAllInputByHeader(QTest_Synchronizer_PATH, Constants.TestExecution, Constants.header_Comments).get(Constants.header_Comments);

		Map<String, Map<String, List<Integer>>> idMap = QTestComponent.getQTestSuiteData();
		for(int i=0; i<listTCDetails.size(); i++ ) {
			TCDetails tc =new TCDetails();
			tc= listTCDetails.get(i);

			Map<String, List<Integer>> idsList =idMap.get(tc.getTestSuiteParentId());
			List<Integer> listRunIds = idsList.get(Constants.TestRunIdMapKey);
			List<Integer> listRunPids = idsList.get(Constants.TestRunPidMapKey);

			JSONObject	proObj = new JSONObject();	
			JSONObject	failureReasonObj = new JSONObject();
			failureReasonObj.put(Constants.FieldId, 6599864);
			failureReasonObj.put(Constants.FieldName, Constants.header_FailureReason);
			failureReasonObj.put(Constants.FieldValue, failureReasonList.get(i));

			JSONObject	testOutputObj = new JSONObject();
			testOutputObj.put(Constants.FieldId , 6434441);
			testOutputObj.put(Constants.FieldName , Constants.header_TestOutput);
			testOutputObj.put(Constants.FieldValue, testOutputList.get(i));	

			JSONObject	commentObj = new JSONObject();
			commentObj.put(Constants.FieldId , 7321256);
			commentObj.put(Constants.FieldName , Constants.header_Comments);
			commentObj.put(Constants.FieldValue, commentList.get(i));

			JSONArray prop= new JSONArray();
			prop.put(failureReasonObj);
			prop.put(testOutputObj);
			prop.put(commentObj);

			proObj.put(Constants.Properties, prop);

			if(listRunPids.contains(tc.getTestRunId()) && tc.getAutExecutionStatus() != null && tc.getAutExecutionStatus() !="" && !("Sucess".equalsIgnoreCase(tc.getSyncQTestStatus()))) {
				RequestSpecification requestSpecification= RestAssured.given().log().all().pathParam(Constants.Test_RunsId, listRunIds.get(i)).					
						headers(Constants.Authorization , Utility.getProperty(Constants.Token)).body(proObj.toString());				
				requestSpecification.contentType(ContentType.JSON);
				Response response = requestSpecification.put(Utility.getProperty(Constants.Url) +  Utility.getProperty(Constants.ProjectId) + Utility.getProperty(Constants.CONTEXT_PATH_TestRun));	
				log.info("Status Code: "+ response.getStatusCode());
				System.out.println("done");
			}
		}
	}


}
































