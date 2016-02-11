
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

public class BinaryAdaBooster_final
{	
	Map<String, Object> hMap = new HashMap<String, Object>();
	static double selectedHypothesis = 0.0;
	static double selectedErrorVal = Integer.MAX_VALUE;
	static double selectedMidVal = 0.0;
	static String selectedoperator;
	static String inputFile = "dataset";
	int hypothesisCount = 0;
	int exampleCount = 0;
	double epsilonValue;
	ArrayList<ArrayList<Integer>> lst_instances = new ArrayList<ArrayList<Integer>>();
	Hashtable<Double, Double> errorLess = new Hashtable<Double, Double>();	
	static ArrayList<String> exampleList;
	static ArrayList<String> outputlist;
	static ArrayList<String> probList;
	int iterationCount = 0;
	Hashtable<Double, Double> errorMore = new Hashtable<Double, Double>(); 
	ArrayList<String[]> boostedClassifierList = new ArrayList<String[]>();

	public static void main(String args[]) throws Exception 
	{
		BinaryAdaBooster_final bAdaBooster = new BinaryAdaBooster_final();
		bAdaBooster.readInputData();
		bAdaBooster.performAdaBoosting();
	}
	@SuppressWarnings("unchecked")
	public void performAdaBoosting(){
		hMap.put("SampleValueList", exampleList);
		hMap.put("ExpectedResultList", outputlist);
		hMap.put("ProbValueList", probList);
		hMap.put("PreNormalizedFactorValueList", probList);
		Map<String, Object> currentHMap = new HashMap<String, Object>();
		for(int i = 0; i < hypothesisCount; i++)
		{			
			System.out.println("Iteration"+ (i+1));
			selectedErrorVal = Integer.MAX_VALUE;
			selectedoperator = "";
			selectedMidVal = 0.0;
			getMinErrorHypothesis();
			currentHMap.put("SampleValueList", hMap.get("SampleValueList"));
			currentHMap.put("ExpectedResultList", hMap.get("ExpectedResultList"));
			currentHMap.put("ProbValueList", hMap.get("ProbValueList"));
			currentHMap.put("ErrorValue", selectedErrorVal);
			currentHMap.put("SelectedClassifier", String.valueOf(selectedMidVal));
			currentHMap.put("OperatorSelected", selectedoperator);
			System.out.println("Classifier h = I(x " +selectedoperator+" "+ selectedMidVal+")");
			System.out.println("Error = "+(selectedErrorVal));
			double alphaValue=0.0;
			double qiValueRight;
			double qiValueWrong;
			alphaValue = computeAlpha(selectedErrorVal);
			System.out.println("Alpha = "+(alphaValue));
			alphaValue = Double.valueOf((alphaValue));
			currentHMap.put("WeightValue", alphaValue);
			qiValueRight = Double.valueOf((computeQiRight(alphaValue)));
			qiValueWrong = Double.valueOf((computeQiWrong(alphaValue)));
			findPreNormalizedPi(currentHMap,  qiValueRight,  qiValueWrong, selectedMidVal);
			double valueZ = Double.valueOf((computeZ(currentHMap)));
			System.out.println("Normalization Factor Z = "+valueZ);
			currentHMap.put("ProbNormalizedFactorValue", valueZ);
			findNormalizedPi(currentHMap);
			///System.out.print("Pi after normalization = ");
			String piValues = "Pi after normalization = ";
			for(int kk = 0; kk < exampleList.size(); kk++)
			{
				ArrayList<String> noramlList = (ArrayList<String>) currentHMap.get("NormalizedFactorValueList");
				piValues = piValues + noramlList.get(kk)+", ";
				
			}		
			if(piValues.trim().endsWith(",")){
			int len = piValues.length();
			piValues = piValues.trim().substring(0, len-2);
			}
			System.out.println(piValues);

			currentHMap.put("ProbValueList", currentHMap.get("NormalizedFactorValueList"));
			String[] boostedClassifierFn = new String[4];
			boostedClassifierFn[0] = alphaValue + "";
			boostedClassifierFn[1] = selectedMidVal + "";
			boostedClassifierFn[2] = selectedoperator + "";
			boostedClassifierFn[3] = valueZ + "";
			boostedClassifierList.add(boostedClassifierFn);
			int errorCount = computeErrorOfBoostedClassifier(boostedClassifierList);
			System.out.print("Boosted Classifier f(x) =");
			for(int k = 0; k < boostedClassifierList.size();k++)
			{
				String[] boostedClassifierFnTemp = boostedClassifierList.get(k);
				System.out.print(" "+boostedClassifierFnTemp[0]+" ");
				System.out.print("* I(x " +boostedClassifierFnTemp[2]+" "+ boostedClassifierFnTemp[1]+")");
				if(boostedClassifierList.size() > 0 && k != boostedClassifierList.size()-1){
					System.out.print(" + ");
				}
			}
			System.out.println();
			System.out.println("Boosted Classifier Error = "+ Double.valueOf(errorCount)/exampleCount);
			currentHMap.put("ErrorOfBoostedClassifier", Double.valueOf(errorCount)/exampleCount);
			double boundValue = computeBoundOnEt(boostedClassifierList);
			System.out.println("Bound on Error = "+boundValue);
			currentHMap.put("BoundOnEt", boundValue);
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

	public int computeErrorOfBoostedClassifier(ArrayList<String[]> boostedClassifierHypoList)
	{
		int errorCount = 0;
		ArrayList<String> inputXValueList = exampleList;
		ArrayList<String> inputYValueList = outputlist;
		for(int i = 0; i < inputXValueList.size(); i++)
		{
			double fOfXFinalValue = 0.0;
			for(int k = 0; k < boostedClassifierHypoList.size(); k++)
			{
				double fOfXValue = 0.0;
				String[] boostedFn = boostedClassifierHypoList.get(k);
				fOfXValue = fOfXValue + Double.parseDouble(boostedFn[0]);				
				double xValue = Double.valueOf(inputXValueList.get(i));
				if(boostedFn[2].equals("<"))
				{
					if(xValue < Double.parseDouble(boostedFn[1]))
					{
						if(inputYValueList.get(i).equals("1"))
						{
							fOfXValue = fOfXValue* 1;
						}
						else
						{
							fOfXValue = fOfXValue* -1;
						}
					}
					if(xValue > Double.parseDouble(boostedFn[1])){
						if(inputYValueList.get(i).equals("-1")){
							fOfXValue = fOfXValue* 1;
						}else{
							fOfXValue = fOfXValue* -1;
						}
					}
				}					
				if(boostedFn[2].equals(">"))
				{
					if(xValue < Double.parseDouble(boostedFn[1]))
					{
						if(inputYValueList.get(i).equals("-1"))
						{
							fOfXValue = fOfXValue* 1;
						}
						else
						{
							fOfXValue = fOfXValue* -1;
						}
					}
					if(xValue > Double.parseDouble(boostedFn[1]))
					{
						if(inputYValueList.get(i).equals("1"))
						{
							fOfXValue = fOfXValue* 1;
						}
						else
						{
							fOfXValue = fOfXValue* -1;
						}
					}					
				}
				fOfXFinalValue = fOfXFinalValue+fOfXValue;
			}

			if(fOfXFinalValue < 0)
			{
				errorCount++;
			}
		}
		return errorCount;
	}

	@SuppressWarnings("unchecked")
	public void findNormalizedPi(Map<String, Object> currentHypothesisMap){
		probList.clear();
		String val = String.valueOf(currentHypothesisMap.get("ProbNormalizedFactorValue"));
		double normalizationFactor = Double.valueOf(val);
		if(normalizationFactor != 0){
			ArrayList<String> factorList = (ArrayList<String>) currentHypothesisMap.get("PreNormalizedFactorValueList");
			for(String prenormValue: factorList){
				double newValue = Double.valueOf(prenormValue)/normalizationFactor;
				probList.add(String.valueOf(newValue));
			}
		}
		currentHypothesisMap.put("NormalizedFactorValueList", probList);
	}

	@SuppressWarnings("unchecked")
	public double computeZ(Map<String, Object> currentHypothesisMap)
	{
		double total = 0.0;
		ArrayList<String> preNormalizedList = (ArrayList<String>) currentHypothesisMap.get("PreNormalizedFactorValueList");
		for(String value: preNormalizedList){

			total = total + Double.valueOf(value);
		}
		return total;
	}

	@SuppressWarnings("unchecked")
	public void findPreNormalizedPi(Map<String, Object> currentHypothesisMap, double qiValueRight, double qiValueWrong, double bestMidValue)
	{
		ArrayList<String> preNormalizedPi = new ArrayList<String>();
		if(String.valueOf(currentHypothesisMap.get("OperatorSelected")).equals("<"))
		{
			ArrayList<String> sampleValueList = (ArrayList<String>) currentHypothesisMap.get("SampleValueList");
			for(int j = 0; j < sampleValueList.size(); j++)
			{
				double probValue;
				String Xvalue1 = sampleValueList.get(j);
				ArrayList<String> expectedList = (ArrayList<String>) currentHypothesisMap.get("ExpectedResultList");
				String Yvalue1 = expectedList.get(j);
				if(Double.valueOf(Xvalue1) < bestMidValue){
					if(Integer.valueOf(Yvalue1) == -1){
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueWrong;
						preNormalizedPi.add(String.valueOf(probValue));
					}
					else{
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueRight;
						preNormalizedPi.add(String.valueOf(probValue));
					}
				}
				if(Double.valueOf(Xvalue1) >= bestMidValue){
					if(Integer.valueOf(Yvalue1) == 1){
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueWrong;
						preNormalizedPi.add(String.valueOf(probValue));
					}
					else{
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueRight;
						preNormalizedPi.add(String.valueOf(probValue));
					}
				}
			}
			currentHypothesisMap.put("PreNormalizedFactorValueList", preNormalizedPi);
		}

		if(String.valueOf(currentHypothesisMap.get("OperatorSelected")).equals(">"))
		{
			ArrayList<String> sampleValueList = (ArrayList<String>) currentHypothesisMap.get("SampleValueList");
			for(int j = 0; j < sampleValueList.size(); j++){

				double probValue;
				String Xvalue1 = sampleValueList.get(j);

				ArrayList<String> expectedList = (ArrayList<String>) currentHypothesisMap.get("ExpectedResultList");
				String Yvalue1 = expectedList.get(j);
				if(Double.valueOf(Xvalue1) > bestMidValue){
					if(Integer.valueOf(Yvalue1) == -1){
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueWrong;
						preNormalizedPi.add(String.valueOf(probValue));
					}
					else{
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueRight;
						preNormalizedPi.add(String.valueOf(probValue));
					}
				}
				if(Double.valueOf(Xvalue1) <= bestMidValue){
					if(Integer.valueOf(Yvalue1) == 1){
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueWrong;
						preNormalizedPi.add(String.valueOf(probValue));
					}
					else{
						ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
						probValue = Double.valueOf(probValueList.get(j)) * qiValueRight;
						preNormalizedPi.add(String.valueOf(probValue));
					}
				}
			}
			currentHypothesisMap.put("PreNormalizedFactorValueList", preNormalizedPi);			
		}

	}

	public double computeAlpha(double ephsilonValue)
	{
		double alphaValue;
		alphaValue = 0.5 * Math.log(((1-ephsilonValue)/ephsilonValue));
		return alphaValue;
	}

	public double computeQiRight(double alphaValue)
	{
		double qiRight = 0.0;
		qiRight = Math.exp(-alphaValue);
		return qiRight;
	}

	public double computeQiWrong(double alphaValue){

		double qiWrong = 0.0;
		qiWrong = Math.exp(alphaValue);
		return qiWrong;
	}

	public static void getMinErrorHypothesis()
	{
		int iCounter = 0, minValue = 0, maxValue = outputlist.size()-1, midValue = minValue + 1;
		double plusLeftError = 0.0,minusLeftError = 0.0,plusRightError = 0.0,minusRightError = 0.0;
		double dTotalErrorCase1 = 0.0,dTotalErrorCase2 = 0.0,dTempError = 0.0;
		String strTempSide = "";
		while((minValue < midValue) && (midValue<= maxValue))
		{
			plusLeftError =0.0;
			minusLeftError =0.0;
			plusRightError =0.0;
			minusRightError =0.0;
			dTempError = 0.0;
			strTempSide = "";
			iCounter = midValue;
			while(iCounter <= maxValue)
			{
				if(Integer.parseInt(outputlist.get(iCounter)) > 0)
				{
					plusRightError +=  Double.parseDouble(probList.get(iCounter));
				}
				iCounter += 1;
			}
			iCounter = 0;
			while(iCounter < midValue)
			{
				if(Integer.parseInt(outputlist.get(iCounter)) < 0)
				{
					minusLeftError += Double.parseDouble(probList.get(iCounter));
				}
				iCounter += 1;
			}

			dTotalErrorCase1 = minusLeftError + plusRightError;
			iCounter = midValue;
			while(iCounter <= maxValue)
			{
				if(Integer.parseInt(outputlist.get(iCounter)) < 0)
				{
					minusRightError += Double.parseDouble(probList.get(iCounter));
				}
				iCounter += 1;
			}
			iCounter = 0;
			while(iCounter < midValue)
			{
				if(Integer.parseInt(outputlist.get(iCounter)) > 0)
				{
					plusLeftError += Double.parseDouble(probList.get(iCounter));
				}
				iCounter += 1;
			}

			dTotalErrorCase2 = plusLeftError + minusRightError;
			if(dTotalErrorCase1 > dTotalErrorCase2)
			{
				strTempSide = ">";
				dTempError = dTotalErrorCase2;
			}
			else
			{
				strTempSide = "<";
				dTempError = dTotalErrorCase1;
			}
			if(dTempError < selectedErrorVal)
			{
				selectedErrorVal = dTempError;
				selectedoperator = strTempSide;
				selectedMidVal =(double)((Double.parseDouble(exampleList.get(minValue)) + Double.parseDouble(exampleList.get(midValue)))/2);
			}
			minValue += 1;
			midValue += 1;
		}
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
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probValueList.get(j));
				}
			}
			else
			{
				if(Integer.valueOf(Yvalue1) == 1)
				{
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probValueList.get(j));

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
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probValueList.get(j));
				}
			}
			else
			{
				if(Integer.valueOf(Yvalue1) == -1)
				{
					ArrayList<String> probValueList = (ArrayList<String>) hMap.get("ProbValueList");
					errorCountValue = errorCountValue+ Double.valueOf(probValueList.get(j));
				}
			}
		}	
		return errorCountValue;		
	}

	@SuppressWarnings("resource")
	public  void readInputData() throws Exception 
	{
		try
		{
			System.out.println(" ");
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
			exampleCount = Integer.parseInt(values[1]);
			epsilonValue = Double.parseDouble(values[2]);
			line = br.readLine();
			inputXValues = line.split(" ");
			for(String xNum : inputXValues){
				inputXValueList.add(xNum);
			}
			line = br.readLine();
			inputYValues = line.split(" ");
			for(String yNum : inputYValues){
				inputYValueList.add(yNum);
			}
			line = br.readLine();
			inputProbValues = line.split(" ");
			for(String yNum : inputProbValues){
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
