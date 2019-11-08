package businessobjects;

import java.io.File;

import mxdev.iface.cron.FlatFileProcess;

public class FlatFileProcessTest {

	public static void main(String[] args)
	{
		String fDir = ".\\test\\testfiles\\";
		
		// simple transformation
		// - remove original header row: linesToSkip=1
		// - add standard maximo header: header1
		// - new columns names: header2
		// - switch columns: colDef
		System.out.println();
		FlatFileProcess ffp1 = new FlatFileProcess(1, "aaa,bbb,,EN", "F2new,F4new,F1new,Date", "2,4,1", ",", "", false);
		ffp1.init();
		ffp1.processCsvFile(new File(fDir+"test1.csv"), new File (fDir+"test1.out.csv"));
		
		// complex transformation
		// column 1 extract substring
		// column 2 extract substring
		// column 3 parse date
		// column 4 current timestamp
		// column 5,6 constants
		System.out.println();
		FlatFileProcess ffp2 = new FlatFileProcess(1, "aaa,bbb,,EN", "F2new,F4new,F1new", "3.1-2,3.2-3,6..DATE,[CURRDATETIME],[testconstnospaces],[test const with spaces]", ",", "dd/MM/yy HH:mm", false);
		ffp2.init();
		ffp2.processCsvFile(new File(fDir+"test1.csv"), new File (fDir+"test2.out.csv"));
		
		System.out.println();
		FlatFileProcess ffp3 = new FlatFileProcess(1, "aaa,bbb,,EN", "F2new,F4new,F1new", "1,3", ",", "", false);
		ffp3.init();
		ffp3.processCsvFile(new File(fDir+"test3.csv"), new File (fDir+"test3.out.csv"));
		
	}

}
