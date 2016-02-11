import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;


public class RealAdaBooster_final
{	
	ArrayList<ArrayList<Integer>> lst_Instances = new ArrayList<ArrayList<Integer>>();
	DecimalFormat df = new DecimalFormat("#.########");
	Map<String, Object> hMap = new HashMap<String, Object>();
	double selectedHypothesis = 10.0;
	double selectedErrorVal = 10.0;
	double selectedMidVal = 10.0;
	String selectedoperator = "<";
	static String inputFile = "dataset";
	int hypothesisCount = 0;
	int numOfExamples;
	double epsilonValue;	
	Hashtable<Double, Double> errorLessThanHashTable = new Hashtable<Double, Double>();
	Hashtable<Double, Double> errorGreaterThanHashTable = new Hashtable<Double, Double>(); 	
	ArrayList<String[]> boostedClassifierHypoList = new ArrayList<String[]>();
	ArrayList<String> exampleList;
	ArrayList<String> outputlist;
	ArrayList<String> probList;
	int iterationCount;

	public static void main(String args[]) throws Exception 
	{		
		RealAdaBooster_final rAdaBooster = new RealAdaBooster_final();
		rAdaBooster.readinputData();
		rAdaBooster.performrealAdaBoosting();
	}
	@SuppressWarnings("unchecked")
	public void performrealAdaBoosting()
	{		
		hMap.put("SampleValueList", exampleList);
		hMap.put("ExpectedResultList", outputlist);
		hMap.put("ProbValueList", probList);
		hMap.put("PreNormalizedFactorValueList", probList);		
		Map<String, Object> currentHMap = new HashMap<String, Object>();		
		for(int i = 0; i < hypothesisCount; i++)
		{
			System.out.println("");
			System.out.println("Iteration "+ (i+1));
			getMinErrorHypothesis();
			currentHMap.put("SampleValueList",hMap.get("SampleValueList"));
			currentHMap.put("ExpectedResultList",hMap.get("ExpectedResultList"));
			currentHMap.put("ProbValueList",hMap.get("ProbValueList"));			
			currentHMap.put("ErrorValue",selectedErrorVal);
			currentHMap.put("SelectedClassifier",String.valueOf(selectedMidVal));
			currentHMap.put("OperatorSelected",selectedoperator);			
			System.out.println("Classifier h = I(x " +selectedoperator+" "+ selectedMidVal+")");			
			findGValue(currentHMap);			
			System.out.println("G error = "+String.valueOf(currentHMap.get("gValue")));
			findCValues(currentHMap);			
			System.out.println("C_Plus = "+String.valueOf(currentHMap.get("Cplus"))
					+" , C_Minus = "+String.valueOf(currentHMap.get("Cminus")));
			findPreNormalizedPi(currentHMap);
			findZValue(currentHMap);			
			System.out.println("Normalization Factor Z = "+String.valueOf(currentHMap.get("ProbNormalizedFactorValue")));			
			findNormalizedPi(currentHMap);			
			currentHMap.put("ProbValueList",currentHMap.get("NormalizedFactorValueList"));			
			String[] boostedClassifierFn = new String[4];
			boostedClassifierFn[0] = String.valueOf(currentHMap.get("gValue")) + "";
			boostedClassifierFn[1] = selectedMidVal + "";
			boostedClassifierFn[2] = selectedoperator + "";
			boostedClassifierFn[3] = String.valueOf(currentHMap.get("ProbNormalizedFactorValue")) + "";
			boostedClassifierHypoList.add(boostedClassifierFn);			
			computefOfxExamples(currentHMap);			
			String piValues = "Pi after normalization = ";
			String piValues1 = "f(x) = ";
			for(int kk = 0; kk < exampleList.size(); kk++){
				ArrayList<String> noramlizedList = (ArrayList<String>) currentHMap.get("NormalizedFactorValueList");
				piValues = piValues + noramlizedList.get(kk)+", ";
				ArrayList<String> fvalueList = (ArrayList<String>) currentHMap.get("fValueList");
				piValues1 = piValues1 + fvalueList.get(kk)+", ";
			}		
			if(piValues.trim().endsWith(",")){
				int len = piValues.length();
				piValues = piValues.trim().substring(0, len-2);
				}
				System.out.println(piValues);
				if(piValues1.trim().endsWith(",")){
					int len = piValues1.length();
					piValues1 = piValues1.trim().substring(0, len-2);
					}
					System.out.println(piValues1);


			int errorCount = computeErrorOfBoostedClassifier(currentHMap);
			currentHMap.put("ErrorOfBoostedClassifier",Double.valueOf(errorCount)/numOfExamples);			
			System.out.println("Boosted Classifier Error = "+ String.valueOf(currentHMap.get("ErrorOfBoostedClassifier")));			
			double boundValue = computeBoundOnEt(boostedClassifierHypoList);			
			System.out.println("Bound on Error = "+boundValue);
			currentHMap.put("The Bound On Et",boundValue);			
			Set<Entry<String, Object>> set = currentHMap.entrySet();
			for (Entry<String, Object> entry : set) 
			{
				hMap.put(entry.getKey(), entry.getValue());
			}	
		}		
	}

	public double computeBoundOnEt(ArrayList<String[]> boostedClassifierHypoList)
	{		
		double zOfXValue = 1.0;		
		for(int k = 0; k < boostedClassifierHypoList.size(); k++)
		{
			String[] boostedFn = boostedClassifierHypoList.get(k);
			zOfXValue = zOfXValue * Double.parseDouble(boostedFn[3]);
		}		
		return zOfXValue;
	}

	@SuppressWarnings("unchecked")
	public int computeErrorOfBoostedClassifier(Map<String, Object> currentHypothesisInfo)
	{		
		int errorCount = 0;
		ArrayList<String> fvalueList = (ArrayList<String>) currentHypothesisInfo.get("fValueList");
		ArrayList<String> expectedList = (ArrayList<String>) currentHypothesisInfo.get("ExpectedResultList");		
		for(int i = 0; i < fvalueList.size(); i++)
		{
			String fValue = fvalueList.get(i);
			String yValue = expectedList.get(i);
			if(String.valueOf(currentHypothesisInfo.get("OperatorSelected")).equals("<"))
			{
				if(Double.valueOf(fValue) < 0)
				{
					if(Double.valueOf(yValue) == 1)
						errorCount++;
				}
				else
				{
					if(Double.valueOf(yValue) == -1)
						errorCount++;
				}
			}			
		}		
		return errorCount;		
	}

	@SuppressWarnings("unchecked")
	public void computefOfxExamples(Map<String, Object> currentHypothesisInfo)
	{		
		ArrayList<String> fValueList = new ArrayList<String>();
		ArrayList<String> lastfValueList = new ArrayList<String>();		
		lastfValueList = (ArrayList<String>) hMap.get("fValueList");
		if (lastfValueList == null) 
		{
			lastfValueList = new ArrayList<String>();
		}
		if(lastfValueList.size() == 0)
		{
			ArrayList<String> sampleList = (ArrayList<String>) hMap.get("SampleValueList");
			for(int i = 0; i < sampleList.size(); i++)
			{
				lastfValueList.add("0.0");
			}
		}		
		if(String.valueOf(currentHypothesisInfo.get("OperatorSelected")).equals("<"))
		{
			ArrayList<String> sampleValueList = (ArrayList<String>) currentHypothesisInfo.get("SampleValueList");
			for(int j = 0; j < sampleValueList.size(); j++)
			{				
				double fValue = 0.0;
				String Xvalue1 = sampleValueList.get(j);
				if(Double.valueOf(Xvalue1) < selectedMidVal)
				{				
					double cp = Double.parseDouble(String.valueOf(currentHypothesisInfo.get("Cplus")));
					fValue = Double.valueOf(lastfValueList.get(j))+cp;
				}
				if(Double.valueOf(Xvalue1) >= selectedMidVal)
				{
					double cp = Double.parseDouble(String.valueOf(currentHypothesisInfo.get("Cminus")));
					fValue = Double.valueOf(lastfValueList.get(j))+cp;
				}
				fValueList.add(df.format(fValue));
			}
			currentHypothesisInfo.put("fValueList",fValueList);			
		}		
		if(String.valueOf(currentHypothesisInfo.get("OperatorSelected")).equals(">"))
		{
			ArrayList<String> sampleValueList = (ArrayList<String>) currentHypothesisInfo.get("SampleValueList");
			for(int j = 0; j < sampleValueList.size(); j++)
			{				
				double fValue = 0.0;
				String Xvalue1 = sampleValueList.get(j);
				if(Double.valueOf(Xvalue1) > selectedMidVal)
				{					
					double cp = Double.parseDouble(String.valueOf(currentHypothesisInfo.get("Cplus")));
					fValue = Double.valueOf(lastfValueList.get(j))+cp;
				}
				if(Double.valueOf(Xvalue1) <= selectedMidVal)
				{
					double cp = Double.parseDouble(String.valueOf(currentHypothesisInfo.get("Cminus")));
					fValue = Double.valueOf(lastfValueList.get(j))+cp;
				}
				fValueList.add(df.format(fValue));
			}
			currentHypothesisInfo.put("fValueList",fValueList);			
		}		
	}

	@SuppressWarnings("unchecked")
	public void findNormalizedPi(Map<String, Object> currentHypothesisInfo){

		ArrayList<String> normalizedPi = new ArrayList<String>();
		double normalizationFactor = Double.valueOf(String.valueOf(currentHypothesisInfo.get("ProbNormalizedFactorValue")));
		//	System.out.println(normalizationFactor);
		if(normalizationFactor != 0){
			ArrayList<String> preNoramlList = (ArrayList<String>) currentHypothesisInfo.get("PreNormalizedFactorValueList");
			for(String prenormValue: preNoramlList){
				double newValue = Double.valueOf(prenormValue)/normalizationFactor;
				normalizedPi.add(df.format(newValue));
			}
		}

		currentHypothesisInfo.put("NormalizedFactorValueList",normalizedPi);
	}


	@SuppressWarnings("unchecked")
	public void findZValue (Map<String, Object> currentHypothesisInfo)
	{		
		double zValue = 0.0;
		ArrayList<String> preNormalList = (ArrayList<String>) currentHypothesisInfo.get("PreNormalizedFactorValueList");
		for(String preProbValue : preNormalList)
		{			
			zValue = zValue + Double.valueOf(preProbValue);			
		}
		currentHypothesisInfo.put("ProbNormalizedFactorValue",zValue);		
	}

	@SuppressWarnings("unchecked")
	public void findPreNormalizedPi(Map<String, Object> currentHypothesisInfo)
	{		
		ArrayList<String> preNormalizedPi = new ArrayList<String>();
		if(String.valueOf(currentHypothesisInfo.get("OperatorSelected")).equals("<"))
		{
			ArrayList<String> sampleValueList = (ArrayList<String>) currentHypothesisInfo.get("SampleValueList");

			for(int j = 0; j < sampleValueList.size(); j++)
			{				
				double probValue = 0.0;
				String Xvalue1 = sampleValueList.get(j);
				ArrayList<String> expectedList = (ArrayList<String>) currentHypothesisInfo.get("ExpectedResultList");
				String Yvalue1 = expectedList.get(j);
				if(Double.valueOf(Xvalue1) < selectedMidVal)
				{				
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					double cp = Double.valueOf(String.valueOf(currentHypothesisInfo.get("Cplus")));
					probValue = Double.valueOf(probValueList.get(j)) * Math.exp(-Double.valueOf(Yvalue1)*cp);
				}
				if(Double.valueOf(Xvalue1) >= selectedMidVal)
				{
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					double cm = Double.valueOf(String.valueOf(currentHypothesisInfo.get("Cminus")));
					probValue = Double.valueOf(probValueList.get(j)) * Math.exp(-Double.valueOf(Yvalue1)*cm);
				}
				preNormalizedPi.add(df.format(probValue));
			}
			currentHypothesisInfo.put("PreNormalizedFactorValueList",preNormalizedPi);			
		}		
		if(String.valueOf(currentHypothesisInfo.get("OperatorSelected")).equals(">"))
		{
			ArrayList<String> sampleValueList = (ArrayList<String>) currentHypothesisInfo.get("SampleValueList");
			for(int j = 0; j < sampleValueList.size(); j++)
			{				
				double probValue = 0.0;
				String Xvalue1 = sampleValueList.get(j);
				ArrayList<String> expectedList = (ArrayList<String>) currentHypothesisInfo.get("ExpectedResultList");				
				String Yvalue1 = expectedList.get(j);
				if(Double.valueOf(Xvalue1) > selectedMidVal)
				{					
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					double cp = Double.valueOf(String.valueOf(currentHypothesisInfo.get("Cplus")));
					probValue = Double.valueOf(probValueList.get(j)) * Math.exp(-Double.valueOf(Yvalue1)*cp);
				}
				if(Double.valueOf(Xvalue1) <= selectedMidVal)
				{
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					double cm = Double.valueOf(String.valueOf(currentHypothesisInfo.get("Cminus")));
					probValue = Double.valueOf(probValueList.get(j)) * Math.exp(-Double.valueOf(Yvalue1)*cm);
				}
				preNormalizedPi.add(df.format(probValue));
			}
			currentHypothesisInfo.put("PreNormalizedFactorValueList",preNormalizedPi);				
		}		
	}

	public void findCValues(Map<String, Object> currentHypothesisInfo)
	{		
		double cPlusValue = 0.0;
		double cMinusValue = 0.0;
		String prVal = String.valueOf(currentHypothesisInfo.get("PrRightplus"));
		double pr = Double.parseDouble(prVal);
		String prmiVal = String.valueOf(currentHypothesisInfo.get("PrWrongminus"));
		double prMi = Double.parseDouble(prmiVal);
		cPlusValue = 0.5* (Math.log((pr+epsilonValue)/(prMi+epsilonValue)));
		String prWrVal = String.valueOf(currentHypothesisInfo.get("PrWrongplus"));
		double prWr = Double.parseDouble(prWrVal);
		String prWrMinVal = String.valueOf(currentHypothesisInfo.get("PrRightminus"));
		double prWrMin = Double.parseDouble(prWrMinVal);
		cMinusValue = 0.5* (Math.log((prWr+epsilonValue)/(prWrMin+epsilonValue)));
		currentHypothesisInfo.put("Cplus",cPlusValue);
		currentHypothesisInfo.put("Cminus",cMinusValue);		
	}

	@SuppressWarnings("unchecked")
	public void findGValue(Map<String, Object> currentHypothesisMap)
	{		
		double gValue = 0.0;
		double prRightplus =0.0;
		double prRightminus = 0.0;
		double prWrongplus = 0.0;
		double prWrongminus = 0.0;		
		if(String.valueOf(currentHypothesisMap.get("OperatorSelected")).equals("<"))
		{
			ArrayList<String> sampleValueList = (ArrayList<String>) currentHypothesisMap.get("SampleValueList");
			for(int i = 0; i < sampleValueList.size(); i++)
			{				
				String xValue = sampleValueList.get(i);
				ArrayList<String> expectedList = (ArrayList<String>) currentHypothesisMap.get("ExpectedResultList");
				String yValue = expectedList.get(i);
				String classifier = String.valueOf(currentHypothesisMap.get("SelectedClassifier"));
				if(Double.valueOf(xValue) < Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == 1)
					{
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prRightplus = prRightplus + Double.valueOf(probValueList.get(i));						
					}
				}
				if(Double.valueOf(xValue) > Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == -1)
					{
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prRightminus = prRightminus + Double.valueOf(probValueList.get(i));					
					}
				}				
				if(Double.valueOf(xValue) > Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == 1)
					{
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prWrongplus = prWrongplus + Double.valueOf(probValueList.get(i));	
					}
				}

				if(Double.valueOf(xValue) < Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == -1)
					{
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prWrongminus = prWrongminus + Double.valueOf(probValueList.get(i));	
					}
				}								
			}						
		}		

		if(String.valueOf(currentHypothesisMap.get("OperatorSelected")).equals(">"))
		{
			ArrayList<String> sampleList = (ArrayList<String>) currentHypothesisMap.get("SampleValueList");			
			for(int i = 0; i < sampleList.size(); i++)
			{				
				String xValue = sampleList.get(i);
				ArrayList<String> expectedList = (ArrayList<String>) currentHypothesisMap.get("ExpectedResultList");
				String yValue = expectedList.get(i);
				String classifier = String.valueOf(currentHypothesisMap.get("SelectedClassifier"));
				if(Double.valueOf(xValue) > Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == 1){
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prRightplus = prRightplus + Double.valueOf(probValueList.get(i));						
					}
				}
				if(Double.valueOf(xValue) < Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == -1)
					{
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prRightminus = prRightminus + Double.valueOf(probValueList.get(i));						
					}
				}

				if(Double.valueOf(xValue) < Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == 1){
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prWrongplus = prWrongplus + Double.valueOf(probValueList.get(i));	
					}
				}

				if(Double.valueOf(xValue) > Double.valueOf(classifier))
				{
					if(Double.valueOf(yValue) == -1)
					{
						ArrayList<String> probValueList = (ArrayList<String>) currentHypothesisMap.get("ProbValueList");
						prWrongminus = prWrongminus + Double.valueOf(probValueList.get(i));	
					}
				}
			}
		}
		gValue = Math.sqrt(prRightplus*prWrongminus)+Math.sqrt(prWrongplus*prRightminus);
		currentHypothesisMap.put("PrRightplus",prRightplus);
		currentHypothesisMap.put("PrRightminus",prRightminus);
		currentHypothesisMap.put("PrWrongplus",prWrongplus);
		currentHypothesisMap.put("PrWrongminus",prWrongminus);
		currentHypothesisMap.put("gValue",gValue);
	}

	public void getMinErrorHypothesis()
	{
		double errorCountValue;
		selectedErrorVal = 10.0;		
		errorLessThanHashTable.clear();
		for(int i = 0; i < exampleList.size()-1; i++)
		{
			String value1 = exampleList.get(i);
			String value2 = "0.0";
			double midValue = 0.0;
			if(i != exampleList.size()-1)
			{
				value2 = exampleList.get(i+1);
				midValue = (Double.valueOf(value1)+Double.valueOf(value2))/2;
			}
			if(i ==0)
			{
				errorCountValue = getGValueLessThan(Double.valueOf(value1)-1.0);
				if(errorCountValue < selectedErrorVal){
					selectedErrorVal = errorCountValue;
					selectedHypothesis = Double.valueOf(value1)-1.0;
					selectedoperator = "<";
					selectedMidVal = Double.valueOf(value1)-1.0;
				}
			}
			if(i == exampleList.size()-1)
			{
				value1 = exampleList.get(i);
				errorCountValue = getGValueLessThan(Double.valueOf(value1)+1.0);
				if(errorCountValue < selectedErrorVal)
				{
					selectedErrorVal = errorCountValue;
					selectedHypothesis = Double.valueOf(value1)+1.0;
					selectedoperator = "<";
					selectedMidVal = Double.valueOf(value1)+1.0;					
				}
			}
			else
			{
				errorCountValue = getGValueLessThan(midValue);
				if(errorCountValue < selectedErrorVal)
				{
					selectedErrorVal = errorCountValue;
					selectedHypothesis = Double.valueOf(value1);
					selectedoperator = "<";
					selectedMidVal = midValue;
				}
			}

		}
		errorGreaterThanHashTable.clear();
		for(int i = 0; i < exampleList.size()-1; i++)
		{
			String value1 = exampleList.get(i);
			String value2 = "0.0";
			double midValue = 0.0;
			if(i != exampleList.size()-1)
			{
				value2 = exampleList.get(i+1);
				midValue = (Double.valueOf(value1)+Double.valueOf(value2))/2;
			}			
			if(i ==0)
			{
				errorCountValue = getGValueGreaterThan(Double.valueOf(value1)-1.0);
				if(errorCountValue < selectedErrorVal)
				{
					selectedErrorVal = errorCountValue;
					selectedHypothesis = Double.valueOf(value1)-1.0;
					selectedoperator = ">";
					selectedMidVal = Double.valueOf(value1)-1.0;					
				}
			}
			if(i == exampleList.size()-1)
			{
				value1 = exampleList.get(i);
				errorCountValue = getGValueGreaterThan(Double.valueOf(value1)+1.0);
				if(errorCountValue < selectedErrorVal){
					selectedErrorVal = errorCountValue;
					selectedHypothesis = Double.valueOf(value1)+1.0;
					selectedoperator = ">";
					selectedMidVal = Double.valueOf(value1)+1.0;					
				}
			}
			else
			{
				errorCountValue = getGValueGreaterThan(midValue);
				if(errorCountValue < selectedErrorVal)
				{
					selectedErrorVal = errorCountValue;
					selectedHypothesis = Double.valueOf(value1);
					selectedoperator = ">";
					selectedMidVal = midValue;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public double getGValueGreaterThan(double midValue)
	{
		double gValue = 0.0;
		double prRightplus = 0.0;
		double prRightminus = 0.0;
		double prWrongplus = 0.0;
		double prWrongminus = 0.0;
		ArrayList<String> sampleList = (ArrayList<String>) hMap.get("SampleValueList");
		for(int i = 0; i < sampleList.size(); i++)
		{
			String xValue = sampleList.get(i);
			ArrayList<String> expectedList = (ArrayList<String>) hMap.get("ExpectedResultList");
			String yValue = expectedList.get(i);
			if(Double.valueOf(xValue) > Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == 1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prRightplus = prRightplus + Double.valueOf(probList.get(i));
					prRightplus = Double.valueOf(df.format(prRightplus));
				}
			}
			if(Double.valueOf(xValue) < Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == -1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prRightminus = prRightminus + Double.valueOf(probList.get(i));
					prRightminus = Double.valueOf(df.format(prRightminus));
				}
			}

			if(Double.valueOf(xValue) < Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == 1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prWrongplus = prWrongplus + Double.valueOf(probList.get(i));
					prWrongplus= Double.valueOf(df.format(prWrongplus));
				}
			}
			if(Double.valueOf(xValue) > Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == -1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prWrongminus = prWrongminus + Double.valueOf(probList.get(i));
					prWrongminus=	Double.valueOf(df.format(prWrongminus));
				}
			}
		}
		gValue = Math.sqrt(prRightplus*prWrongminus)+Math.sqrt(prWrongplus*prRightminus);
		return gValue;
	}

	@SuppressWarnings("unchecked")
	public double getGValueLessThan(double midValue)
	{
		double gValue = 0.0;
		double prRightplus =0.0;
		double prRightminus = 0.0;
		double prWrongplus = 0.0;
		double prWrongminus = 0.0;
		ArrayList<String> sampleList = (ArrayList<String>) hMap.get("SampleValueList");
		for(int i = 0; i < sampleList.size(); i++)
		{
			String xValue = sampleList.get(i);
			ArrayList<String> expectedList = (ArrayList<String>) hMap.get("ExpectedResultList");
			String yValue = expectedList.get(i);
			if(Double.valueOf(xValue) < Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == 1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prRightplus = prRightplus + Double.valueOf(probList.get(i));
					prRightplus = Double.valueOf(df.format(prRightplus));
				}
			}
			if(Double.valueOf(xValue) > Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == -1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prRightminus = prRightminus + Double.valueOf(probList.get(i));
					prRightminus = Double.valueOf(df.format(prRightminus));	
				}
			}
			if(Double.valueOf(xValue) > Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == 1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prWrongplus = prWrongplus + Double.valueOf(probList.get(i));
					prWrongplus= Double.valueOf(df.format(prWrongplus));	
				}
			}

			if(Double.valueOf(xValue) < Double.valueOf(midValue))
			{
				if(Double.valueOf(yValue) == -1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					prWrongminus = prWrongminus + Double.valueOf(probList.get(i));
					prWrongminus=	Double.valueOf(df.format(prWrongminus));
				}
			}								
		}
		gValue = Math.sqrt(prRightplus*prWrongminus)+Math.sqrt(prWrongplus*prRightminus);
		return gValue;
	}

	@SuppressWarnings("unchecked")
	public double getErrorsCountIfLessThan(double midValue)
	{
		double errorCountValue = 0.0;
		for(int j = 0; j < exampleList.size(); j++)
		{
			String Xvalue1 = exampleList.get(j);
			String Yvalue1 = outputlist.get(j);
			if(Double.valueOf(Xvalue1) < midValue)
			{
				if(Integer.valueOf(Yvalue1) == -1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probList.get(j));
				}
			}
			else
			{
				if(Integer.valueOf(Yvalue1) == 1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probList.get(j));
				}
			}
		}
		return errorCountValue;
	}

	@SuppressWarnings("unchecked")
	public double getErrorsCountIfGreaterThan(double midValue)
	{
		double errorCountValue = 0.0;
		for(int j = 0; j < exampleList.size(); j++)
		{
			String Xvalue1 = exampleList.get(j);
			String Yvalue1 = outputlist.get(j);
			if(Double.valueOf(Xvalue1) < midValue)
			{
				if(Integer.valueOf(Yvalue1) == 1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probList.get(j));
				}
			}
			else
			{
				if(Integer.valueOf(Yvalue1) == -1)
				{
					ArrayList<String> probList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probList.get(j));
				}
			}
		}
		return errorCountValue;
	}

	@SuppressWarnings("resource")
	public  void readinputData() throws Exception 
	{
		try
		{
			System.out.println("Enter the file name");
			Scanner in = new Scanner(System.in);
			FileReader fr = new FileReader(in.nextLine());
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			ArrayList<String> inputXValueList = new ArrayList<String>();
			ArrayList<String> inputYValueList = new ArrayList<String>();
			ArrayList<String> inputProbValueList = new ArrayList<String>();
			String inputXValues[];
			String inputYValues[];
			String inputProbValues[];
			line = br.readLine();
			String values[] = line.split(" ");
			hypothesisCount = Integer.parseInt(values[0]);
			numOfExamples = Integer.parseInt(values[1]);
			epsilonValue = Double.parseDouble(values[2]);
			line = br.readLine();
			inputXValues = line.split(" ");
			for(String xNum : inputXValues)
			{
				inputXValueList.add(xNum);
			}
			line = br.readLine();
			inputYValues = line.split(" ");
			for(String yNum : inputYValues)
			{
				inputYValueList.add(yNum);
			}
			line = br.readLine();
			inputProbValues = line.split(" ");
			for(String yNum : inputProbValues)
			{
				inputProbValueList.add(yNum);
			}
			outputlist = inputYValueList;
			probList = inputProbValueList;
			exampleList = inputXValueList;
		}
		catch(Exception e)
		{
			System.out.println("No Dataset");
			System.exit(1);
		}
	}
}
