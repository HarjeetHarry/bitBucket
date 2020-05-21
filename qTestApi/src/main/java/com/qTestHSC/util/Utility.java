package com.qTestHSC.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.qTestHSC.core.Constants;
import com.qTestHSC.core.TCDetails;


public class Utility {


	/**
	 * Reading data from excel sheet by passing headers
	 * @param excelPath
	 * @param header
	 * @param sheetName
	 * @return
	 */

	public static Map<String, List<String>> getAllInputByHeader(String excelPath, String sheetName, String... headers)  {

		Map<String, List<String>> mp = new LinkedHashMap<String, List<String>>();

		for (String header : headers) {				
			ArrayList<String> allColumnVal = new ArrayList<String>();
			Workbook workbook;
			try {		
				FileInputStream fis = new FileInputStream(excelPath);	

				if(excelPath.contains("xlsx")) {
					workbook = new XSSFWorkbook(fis);
				}else {
					workbook = new HSSFWorkbook();
				}


				Sheet sheet = workbook.getSheet(sheetName);
				Row headerRow = sheet.getRow(0);
				short cellCount = headerRow.getLastCellNum();
				int cellNo=0;
				for (int i = 0; i < cellCount; i++) {
					Cell headerCell = headerRow.getCell(i);
					String   excelHeader = headerCell.getStringCellValue();	
					if(header.equals(excelHeader))	{
						cellNo=i;
						break;
					}
				}

				for (int i = 1; i <= sheet.getLastRowNum(); i++) {
					Row rw = sheet.getRow(i);
					if(rw != null) {
						Cell cell = rw.getCell(cellNo);
						if(cell != null) {
							String cellValue = cell.getStringCellValue(); 
							allColumnVal.add(cellValue);
						}}
				}}

			catch (FileNotFoundException f) {
				f.printStackTrace();
			}	
			catch (IOException e) {
				e.printStackTrace();
			}
			mp.put(header, allColumnVal);  

		}

		return mp;
	}




	public static int getHederIndex( String excelPath ,String sheetName , String header) {
		try {
			FileInputStream fis = new FileInputStream(excelPath);			
			Workbook workbook = new XSSFWorkbook(fis);
			Sheet sheet = workbook.getSheet(sheetName);
			Row headerRow = sheet.getRow(0);
			short cellCount = headerRow.getLastCellNum();

			for (int i = 0; i < cellCount; i++) {
				Cell header_cell = headerRow.getCell(i);
				String   excelHeader = header_cell.getStringCellValue();	
				if(header.trim().equals(excelHeader.trim()))	{
					return i;			
				}}		
		}

		catch (FileNotFoundException f) {
			f.printStackTrace();
		}	
		catch (IOException e) {

			e.printStackTrace();
		}
		return -1;
	}




	public static void addFixHeaders(String excelPath, String sheetName) {
				
		try {

			FileInputStream webdata = new FileInputStream(excelPath);	
			Workbook wb;
			if(excelPath.contains("xlsx")) {
				wb = new XSSFWorkbook(webdata);
			}else {
				wb = new HSSFWorkbook();
			}
			Sheet sh=wb.getSheet(sheetName);
			Row row = sh.getRow(0);
			FileOutputStream wbData=new FileOutputStream(excelPath);	
			int cellCount = row.getLastCellNum();
			

			row.createCell(cellCount).setCellValue(Constants.header_ExecutionStatus);	
			row.createCell(cellCount + 1).setCellValue(Constants.header_TestOutput);
			row.createCell(cellCount + 2).setCellValue(Constants.header_FailureReason);
			row.createCell(cellCount + 3).setCellValue(Constants.header_Comments);
			row.createCell(cellCount + 4).setCellValue(Constants.header_TimeStamp);
			row.createCell(cellCount + 5).setCellValue(Constants.header_qTestStatus);
			row.createCell(cellCount + 6).setCellValue(Constants.header_qTestFailureReason);

			wb.write(wbData);
			wb.close();
			
		}
		catch (FileNotFoundException f) {
			f.printStackTrace();
		}	
		catch (IOException e) {

			e.printStackTrace();
		}

		

	}

	public static void writeOutputToExcel(String excelPath, String sheetName ,TCDetails tc, Map<String, String> headers) {

		for (Map.Entry<String, String> header : headers.entrySet()) {			

			int colNo= getHederIndex(sheetName, excelPath, header.getKey());
			if (colNo >= 0) {
				Workbook wb;
				try {		
					FileInputStream input=new FileInputStream(excelPath);
					if(excelPath.contains("xlsx")) {
						wb = new XSSFWorkbook(input);
					}else {
						wb = new HSSFWorkbook();
					}					
					Sheet sh=wb.getSheet(sheetName);

					int counter=0;
					for (int i = 1; i <=sh.getLastRowNum(); i++) {
						
						Row row=sh.getRow(i);
						int countryHeaderIndex	= getHederIndex(sheetName, excelPath, "Test Suite Name");
						int tcHeaderIndex = getHederIndex(sheetName, excelPath, "Test Case Id");						

						if( tcHeaderIndex>=0 && row.getCell(tcHeaderIndex).getStringCellValue()!= null 
								&& row.getCell(tcHeaderIndex).getStringCellValue().equalsIgnoreCase(tc.getTestCaseId()) 
								&& countryHeaderIndex >=0 && row.getCell(countryHeaderIndex).getStringCellValue() != null 
								&& row.getCell(countryHeaderIndex).getStringCellValue().equalsIgnoreCase(tc.getTestSuiteName()) ) {

							FileOutputStream webdata=new FileOutputStream(excelPath);
							row.createCell(colNo).setCellValue(header.getValue());
							wb.write(webdata);		
							counter++;
						} 
					}
					if(counter>1) {
						System.out.println("More then 1 record found for this crieteria" 
					    + "For Test Case:" + tc.getTestCaseId() + " & For Test Suite:" + tc.getTestSuiteName());
						
					}
					wb.close();
				}
				catch(Exception e){
					e.printStackTrace();
				}			
			}
		}
	}


	public static String getProperty(String key) {
		String value = "";		
		Properties prop = new Properties();		
		try {
			prop.load(new FileInputStream(System.getProperty(Constants.UserDir) + Constants.ConfigFilePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {              
			e.printStackTrace();
		}

		if(key!=null) {
			value = prop.getProperty(key);
		}

		return value;
	}

	public static List<TCDetails> loadInputDataSheet(String excelPath, String sheetName) {

		List<TCDetails> list= new ArrayList<TCDetails>();

		try {		
			FileInputStream fis = new FileInputStream(excelPath);			
			Workbook workbook = new XSSFWorkbook(fis);
			Sheet sheet = workbook.getSheet(sheetName);

			int rowCount =sheet.getLastRowNum();

			if(rowCount>0) {

				Row headerRow = sheet.getRow(0);

				int cellCount = headerRow.getLastCellNum();
				int i=1;
				while (i< rowCount)
				{
					Row rw= sheet.getRow(i);
					TCDetails tc= new TCDetails();

					for(int j=0; j<cellCount; j++) {

						Cell cl= rw.getCell(j);
						Cell clHeader = headerRow.getCell(j);

						if(cl != null && Constants.header_TestSuiteName.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setTestSuiteName(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_TestSuiteParentId.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setTestSuiteParentId(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_TestCaseId.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setTestCaseId(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_TestRunId.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setTestRunId(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_ExecutionStatus.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setAutExecutionStatus(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_FailureReason.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setAutFailureReason(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_TestOutput.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setAutTestOutput(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_Comments.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setAutComments(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_TimeStamp.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setSyncQTestTimeStamp(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_qTestStatus.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setSyncQTestStatus(cl.getStringCellValue());
						}
						else if(cl != null && Constants.header_qTestFailureReason.equalsIgnoreCase(clHeader.getStringCellValue())) {
							tc.setSyncQTestFailureReason(cl.getStringCellValue());
						}

					}
					list.add(tc);
					i++;
				}
			}
		}		
		catch(Exception e){
			e.printStackTrace();
		}
		return list;

	}

	
	public static void processAutomationDatafromExcel() {
		Map<String, List<String>> autData = getAllInputByHeader(Utility.getProperty(Constants.AUT_ExcelPath) , "Resultdata", "TestCaseId_InTestPlan","Status", "SANorCaseID", "ReasonsForFailure","Test Suite Name");
		List<String> testCaseIds = autData.get("TestCaseId_InTestPlan");
		List<String> countryNames = autData.get("Test Suite Name");
		List<String> statusList = autData.get("Status");
		List<String> testOutputList = autData.get("SANorCaseID");
		List<String> failureList = autData.get("ReasonsForFailure");	

		addFixHeaders( Utility.getProperty(Constants.QTest_Synchronizer_Path), "RL-19 SDS 4.9 P11.4_1");

		for(int i=0; i< testCaseIds.size() ; i++) {
			if(testCaseIds.get(i).contains("TC")) {
				String temp = testCaseIds.get(i);
				String sub = temp.substring(0, temp.indexOf("_"));
				String[] parts = sub.split("-");
			//	System.out.println(sub);

				TCDetails tc = new TCDetails();
				if(parts.length > 1) {
					for(int j=1; j< parts.length ; j++) {
				//		System.out.println(parts[j]);

						tc.setTestCaseId("TC-" + parts[j]);
			
						tc.setTestSuiteName(countryNames.get(i));
						if(countryNames.get(i) == null ) {
							continue;							
						}
					}
					Map<String, String> outputHeadersQTest = new LinkedHashMap<String, String>();
					outputHeadersQTest.put("Execution Status", statusList.get(i));
					outputHeadersQTest.put("Test Output", testOutputList.get(i));
					outputHeadersQTest.put("Failure Reason", failureList.get(i));

	writeOutputToExcel(Utility.getProperty(Constants.QTest_Synchronizer_Path) , "RL-19 SDS 4.9 P11.4_1", tc, outputHeadersQTest);



				}




			}

		}
	}		
}





















