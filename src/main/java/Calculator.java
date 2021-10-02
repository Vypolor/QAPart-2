import com.sun.org.apache.bcel.internal.Const;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Calculator {

    private static double calculatedK = 0.0;
    private static double calculatedN0 = 0.0;
    private static double lastErrorsCountPerDay = 0.0;

    private static class CalculateKIntermediateResultRecord
    {
        double leftSum;
        double rightSum;
        double deltaSum;
        double k;

        public CalculateKIntermediateResultRecord(double leftSum, double rightSum, double k) {
            this.leftSum = leftSum;
            this.rightSum = rightSum;
            this.deltaSum = Math.abs(rightSum - leftSum);
            this.k = k;
        }

        public double getLeftSum() {
            return leftSum;
        }

        public double getRightSum() {
            return rightSum;
        }

        public double getDeltaSum() {
            return deltaSum;
        }

        public double getK() {
            return k;
        }
    }

    private static double calculateA(Map<Integer, Integer> inputValues, double calculatedB){
        AtomicInteger sumDays = new AtomicInteger();
        AtomicReference<Double> sumErrorsPerTime = new AtomicReference<>(0.0);

        inputValues.forEach((curTime, curErrors) -> {
            sumDays.addAndGet(curTime);
            sumErrorsPerTime.updateAndGet(v -> v + curErrors);
        });

        double numerator = sumErrorsPerTime.get() - calculatedB * sumDays.get();

        return numerator / 7;
    }

    private static double calculateB(Map<Integer, Integer> inputValues){

        AtomicReference<Double> diminutiveInNumerator = new AtomicReference<>(0.0);
        AtomicInteger sumDays = new AtomicInteger();
        AtomicReference<Double> sumErrorsPerTime = new AtomicReference<>(0.0);
        AtomicInteger sumSquareDays = new AtomicInteger();

        inputValues.forEach((curTime, curErrors) -> {
            diminutiveInNumerator.updateAndGet(v -> v + curTime * curErrors);
            sumDays.addAndGet(curTime);
            sumErrorsPerTime.updateAndGet(v -> v + curErrors);
            sumSquareDays.addAndGet((int) Math.pow(curTime, 2));
        });

        double numerator = 7 * diminutiveInNumerator.get() - sumDays.get() * sumErrorsPerTime.get();
        double denominator = 7 * sumSquareDays.get() - Math.pow(sumDays.get(), 2);

        return numerator/denominator;
    }

    private static HSSFWorkbook calculateKAndGatherStatisticTable(Map<Integer, Integer> inputValues){

        List<CalculateKIntermediateResultRecord> calculateKStatistic = new ArrayList<>();

        double k = Consts.K;

        double leftSum;
        double rightSum;
        double deltaSum;

        double iterNumber = 0.0;
        while (true){

            ++iterNumber;
            leftSum = calculateLeftSum(inputValues, k);
            rightSum = calculateRightSum(inputValues, k);

            calculateKStatistic.add(new CalculateKIntermediateResultRecord(leftSum, rightSum, k));

            deltaSum = leftSum - rightSum;

            if (Math.abs(deltaSum) < Consts.EPSILON) {
                break;
            }

            k = changeKValue(leftSum, rightSum, k, iterNumber); // Why K changing if we have correct delta already?
        }
        System.out.println(iterNumber);

        calculatedK = k;

        return gatherStatisticForKCalculation(calculateKStatistic);
    }

    private static double calculateLeftSum(Map<Integer, Integer> inputValues, double intermediateK){

        double sum = 0.0;

        for (Map.Entry<Integer, Integer> current : inputValues.entrySet()) {

            double exp = Math.pow(Math.E, intermediateK * current.getKey() * (-1));
            //sum += current.getValue() * current.getKey() * exp; // [dmso0321] e^(-Kt) * n * t: are you sure?
            sum += current.getValue() * exp;
        }

        return sum;
    }

    private static double calculateRightSum(Map<Integer, Integer> inputValues, double intermediateK){

        double sumNumerator1 = 0.0;
        double sumNumerator2 = 0.0;
        double sumDenominator = 0.0;


        for (Map.Entry<Integer, Integer> current : inputValues.entrySet()) {
            double exp1 = Math.pow(Math.E, intermediateK * current.getKey() * (-1)); // e^(-Kt)
            double exp2 = Math.pow(Math.E, intermediateK * current.getKey() * (-2)); // e^(-2Kt)

            // sumNumerator1 += current.getValue() * exp1; // [dmso0321] n*e^(-Kt) I don't think so...
            sumNumerator1 += current.getValue() * exp1 * current.getKey(); // [dmso0321] n * e^(-Kt) * t
            /*sumNumerator2*/sumDenominator += current.getKey() * exp2;   // e^(-2Kt) * t
            /*sumDenominator*/sumNumerator2 += exp2; // e^(-2Kt)
        }

        return (sumNumerator1 * sumNumerator2) / sumDenominator; // [dmso0321] Why? {[n*e^(-Kt)] * [t * e^(-2Kt)]}/[e^(-2Kt)] I think, it's not correct


    }

    private static double changeKValue(double leftSum, double rightSum, double k, double iterNumber){
        if(rightSum > leftSum){
            return k - 1.0/iterNumber;
        }
        else {
            return k + 1.0/iterNumber;
        }
    }

    private static void calculateAndCacheN0(Map<Integer, Integer> inputValues, double resultK){
        double numerator = 0.0;
        double denumerator = 0.0;

        for (Map.Entry<Integer, Integer> current : inputValues.entrySet()) {
            double exp1 = Math.pow(Math.E, resultK * current.getKey() * (-1));
            double exp2 = Math.pow(Math.E, resultK * current.getKey() * (-2));

           numerator += current.getValue() * current.getKey() * exp1;
           denumerator += exp2 * current.getKey();
        }

        denumerator *= resultK;
        calculatedN0 = numerator/denumerator;
    }

    // Statistic of K calculation
    private static HSSFWorkbook gatherStatisticForKCalculation(List<CalculateKIntermediateResultRecord> calculateKStatistic){
        // Excel file
        HSSFWorkbook workbook = new HSSFWorkbook();

        // Create sheet with 'Statistic' name
        HSSFSheet sheet = workbook.createSheet("Statistic");

        AtomicInteger rowNum = new AtomicInteger();

        // Headers
        Row row = sheet.createRow(rowNum.get());
        row.createCell(0).setCellValue("Iteration number");
        row.createCell(1).setCellValue("Left sum");
        row.createCell(2).setCellValue("Right sum");
        row.createCell(3).setCellValue("Delta sum");
        row.createCell(4).setCellValue("Intermediate K");

        rowNum.incrementAndGet();

        calculateKStatistic.forEach(currentRecord -> {
            Row currentRow = sheet.createRow(rowNum.get());
            currentRow.createCell(0).setCellValue(rowNum.get());
            currentRow.createCell(1).setCellValue(currentRecord.getLeftSum());
            currentRow.createCell(2).setCellValue(currentRecord.getRightSum());
            currentRow.createCell(3).setCellValue(currentRecord.getDeltaSum());
            currentRow.createCell(4).setCellValue(currentRecord.getK());
            rowNum.incrementAndGet();
        });

        return workbook;
    }

    // Table for graph of changes in the number of detected errors over time
    private static void createErrorsPerTimeGraphTable(HSSFWorkbook lastModifiedWorkbook, Map<Integer, Integer> inputValues){

        // Create sheet with 'Graph' name
        HSSFSheet sheet = lastModifiedWorkbook.createSheet("Graph");

        AtomicInteger rowNum = new AtomicInteger();

        // Headers
        Row row = sheet.createRow(rowNum.get());
        row.createCell(0).setCellValue("Time");
        row.createCell(1).setCellValue("Detected errors count");

        rowNum.incrementAndGet();

        inputValues.forEach((curTime, curErrors) ->{
            Row currentRow = sheet.createRow(rowNum.get());
            currentRow.createCell(0).setCellValue(curTime);
            currentRow.createCell(1).setCellValue(curErrors);
            rowNum.incrementAndGet();
        });

    }

    // Least square method calculation table
    private static void createLSMTable(HSSFWorkbook lastModifiedWorkbook, Map<Integer, Integer> inputValues){

        AtomicReference<Double> discrepancySquaresSum = new AtomicReference<>(0.0);

        // Create sheet with 'LSM' name
        HSSFSheet sheet = lastModifiedWorkbook.createSheet("LSM");

        AtomicInteger rowNum = new AtomicInteger();

        // Headers
        Row row = sheet.createRow(rowNum.get());
        row.createCell(0).setCellValue("Day");
        row.createCell(1).setCellValue("Delta errors count per day");
        row.createCell(2).setCellValue("Delta errors count per day (calc)");
        row.createCell(3).setCellValue("Discrepancy");
        row.createCell(4).setCellValue("Square discrepancy");
        rowNum.incrementAndGet();

        inputValues.forEach((curTime, curErrors) ->{
            Row currentRow = sheet.createRow(rowNum.get());
            currentRow.createCell(0).setCellValue(curTime);
            currentRow.createCell(1).setCellValue(curErrors);

            double errorsPerDayCalc = calculatedN0 * calculatedK * 1 * Math.pow(Math.E, calculatedK * curTime * (-1));
            lastErrorsCountPerDay = errorsPerDayCalc;
            currentRow.createCell(2).setCellValue(errorsPerDayCalc);

            double discrepancy = curErrors - errorsPerDayCalc;
            currentRow.createCell(3).setCellValue(discrepancy);
            discrepancySquaresSum.updateAndGet(v -> v + Math.pow(discrepancy, 2.0));
            currentRow.createCell(4).setCellValue(Math.pow(discrepancy, 2.0));

            rowNum.incrementAndGet();
        });



        Row lastRow = sheet.createRow(rowNum.get());

        lastRow.createCell(0).setCellValue("Sum of discrepancy squares");
        lastRow.createCell(1).setCellValue(discrepancySquaresSum.get());

    }

    private static void createLinearInterpolationTable(HSSFWorkbook lastModifiedWorkbook, Map<Integer, Integer> inputValues){

        AtomicReference<Double> discrepancySquaresSum = new AtomicReference<>(0.0);
        // Create sheet with 'Interpolation and extrapolation' name
        HSSFSheet sheet = lastModifiedWorkbook.createSheet("Interpolation and extrapolation");

        AtomicInteger rowNum = new AtomicInteger();

        // Headers
        Row row = sheet.createRow(rowNum.get());
        row.createCell(0).setCellValue("Day");
        row.createCell(1).setCellValue("Delta errors count per day");
        row.createCell(2).setCellValue("Delta errors count per day (calc)");
        row.createCell(3).setCellValue("Discrepancy");
        row.createCell(4).setCellValue("Square discrepancy");
        rowNum.incrementAndGet();

        // First row (because of time starts from 0)

        Map<Integer, Integer> inputValuesWithZero = new HashMap<>();
        inputValuesWithZero.put(0, 0);
        inputValuesWithZero.putAll(inputValues);

        double B = calculateB(inputValuesWithZero);
        double A = calculateA(inputValuesWithZero, B);
        System.out.println(A);
        System.out.println(B);

        inputValuesWithZero.forEach((curTime, curErrors) ->{
            Row currentRow = sheet.createRow(rowNum.get());
            currentRow.createCell(0).setCellValue(curTime);
            currentRow.createCell(1).setCellValue(curErrors);

            double errorsPerDayCalc = A + B * curTime;
            currentRow.createCell(2).setCellValue(errorsPerDayCalc);

            double discrepancy = curErrors - errorsPerDayCalc;
            currentRow.createCell(3).setCellValue(discrepancy);
            currentRow.createCell(4).setCellValue(Math.pow(discrepancy, 2));

            discrepancySquaresSum.updateAndGet(v -> v + Math.pow(discrepancy, 2.0));

            rowNum.incrementAndGet();
        });

        Row lastRow = sheet.createRow(rowNum.get());

        lastRow.createCell(0).setCellValue("Sum of discrepancy squares");
        lastRow.createCell(1).setCellValue(discrepancySquaresSum.get());
    }

    private static void createDebuggingCompletionPredictionTable(HSSFWorkbook lastModifiedWorkbook, double probabilityGoal){
        // Create sheet with 'Debugging completion time prediction' name
        HSSFSheet sheet = lastModifiedWorkbook.createSheet("Debugging completion time prediction");

        int rowNum = 0;

        // Headers
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue("Last errors count per day");
        row.createCell(1).setCellValue("N0");
        row.createCell(2).setCellValue("K");
        row.createCell(3).setCellValue("Probability of no errors");
        row.createCell(4).setCellValue("Predicted debugging days count");
        ++rowNum;

        double currentProbability = 0.0;
        int predictableDaysCountForDebuggingCompletion = 7;

        while (probabilityGoal > currentProbability)
        {
            ++predictableDaysCountForDebuggingCompletion;
            currentProbability = 1 - (calculatedN0*calculatedK*Math.pow(Math.E, calculatedK * predictableDaysCountForDebuggingCompletion * (-1))/calculatedK);

            if (predictableDaysCountForDebuggingCompletion > Consts.MAX_PREDICTABLE_DAYS_COUNT_FOR_DEBUGGING_COMPLETION_BORDER){
                System.out.println("Very long debugging... Please reduce the probability of no error");
            }
        }


        row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(lastErrorsCountPerDay);
        row.createCell(1).setCellValue(calculatedN0);
        row.createCell(2).setCellValue(calculatedK);
        row.createCell(3).setCellValue(currentProbability);
        if (predictableDaysCountForDebuggingCompletion > Consts.MAX_PREDICTABLE_DAYS_COUNT_FOR_DEBUGGING_COMPLETION_BORDER)
        {
            row.createCell(4).setCellValue("Years and years...");
        }
        else {
            row.createCell(4).setCellValue(predictableDaysCountForDebuggingCompletion);
        }

    }

    // Creates full Excel file with some sheets
    public static void createReport(Map<Integer, Integer> inputValues){
        // Excel file
        HSSFWorkbook workbook = calculateKAndGatherStatisticTable(inputValues);
        calculateAndCacheN0(inputValues, calculatedK);
        createErrorsPerTimeGraphTable(workbook, inputValues);
        createLSMTable(workbook, inputValues);
        createLinearInterpolationTable(workbook,inputValues);
        createDebuggingCompletionPredictionTable(workbook, 0.999);

        // Save excel
        try (FileOutputStream out = new FileOutputStream(new File(PathConsts.PATH_TO_SAVE_REPORT_FILE))) {
            workbook.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Excel saved!");
    }
}
