import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Calculator {

    private static double calculatedK = 0.0;
    private static double calculatedN0 = 0.0;
    private static double lastErrorsCountPerDay = 0.0;

    private static void calculateKAndGatherStatisticTable(HSSFWorkbook lastModifiedWorkbook, Map<Integer, Integer> inputValues){

        double k = Consts.START_K;

        double leftSum;
        double rightSum;

        double iterNumber = 0.0;

        // Create sheet with 'Statistic' name
        HSSFSheet sheet = lastModifiedWorkbook.createSheet("Statistic");

        int rowNum = 0;

        // Headers
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue("Iteration number");
        row.createCell(1).setCellValue("Left sum");
        row.createCell(2).setCellValue("Right sum");
        row.createCell(3).setCellValue("Delta sum");
        row.createCell(4).setCellValue("Intermediate K");

        ++rowNum;

        while (true){

            ++iterNumber;
            leftSum = calculateLeftSum(inputValues, k);
            rightSum = calculateRightSum(inputValues, k);

            Row currentRow = sheet.createRow(rowNum);
            currentRow.createCell(0).setCellValue(rowNum);
            currentRow.createCell(1).setCellValue(calculateLeftSum(inputValues, k));
            currentRow.createCell(2).setCellValue(calculateRightSum(inputValues, k));
            currentRow.createCell(3).setCellValue(leftSum - rightSum);
            currentRow.createCell(4).setCellValue(k);
            ++rowNum;

            if (Math.abs(leftSum - rightSum) < Consts.EPSILON) {
                break;
            }

            k = changeKValue(leftSum, rightSum, k, iterNumber);
        }
        System.out.println(iterNumber);

        calculatedK = k;

        calculateAndCacheN0(inputValues, calculatedK);
    }

    private static double calculateLeftSum(Map<Integer, Integer> inputValues, double intermediateK){

        double sum = 0.0;

        for (Map.Entry<Integer, Integer> current : inputValues.entrySet()) {

            double exp = Math.pow(Math.E, intermediateK * current.getKey() * (-1));
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

            sumNumerator1 += current.getValue() * exp1 * current.getKey();
            sumDenominator += current.getKey() * exp2;
            sumNumerator2 += exp2;
        }

        return (sumNumerator1 * sumNumerator2) / sumDenominator;


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

    private static void createLSMTableExtended(HSSFWorkbook lastModifiedWorkbook, Map<Integer, Integer> inputValues, int daysBorder){

        // Create sheet with 'LSM' name
        HSSFSheet sheet = lastModifiedWorkbook.createSheet("Extended LSM");

        int rowNum = 0;

        // Headers
        Row row = sheet.createRow(rowNum);

        row.createCell(0).setCellValue("Day");
        row.createCell(1).setCellValue("Delta errors count per day");
        row.createCell(2).setCellValue("Delta errors count per day (calc)");

        ++rowNum;

        for (int curDay = 0; curDay < daysBorder; ++curDay){

            Row currentRow = sheet.createRow(rowNum);

            currentRow.createCell(0).setCellValue(curDay);

            if (inputValues.containsKey(curDay)){
                currentRow.createCell(1).setCellValue(inputValues.get(curDay));
            }

            double errorsPerDayCalc = calculatedN0 * calculatedK * 1 * Math.pow(Math.E, calculatedK * curDay * (-1));
            currentRow.createCell(2).setCellValue(errorsPerDayCalc);

            ++rowNum;
        }

    }

    private static double calculateA(Map<Integer, Integer> inputValuesWithZero, double calculatedB){
        AtomicInteger sumDays = new AtomicInteger();
        AtomicReference<Double> sumErrorsPerTime = new AtomicReference<>(0.0);

        inputValuesWithZero.forEach((curTime, curErrors) -> {
            sumDays.addAndGet(curTime);
            sumErrorsPerTime.updateAndGet(v -> v + curErrors);
        });

        double numerator = sumErrorsPerTime.get() - calculatedB * sumDays.get();

        return numerator / Consts.CURRENT_DAYS_COUNT;
    }

    private static double calculateB(Map<Integer, Integer> inputValuesWithZero){

        AtomicInteger diminutiveInNumerator = new AtomicInteger();
        AtomicInteger sumDays = new AtomicInteger();
        AtomicInteger sumErrorsPerTime = new AtomicInteger();
        AtomicInteger sumSquareDays = new AtomicInteger();

        inputValuesWithZero.forEach((curTime, curErrors) -> {
            diminutiveInNumerator.updateAndGet(v -> v + curTime * curErrors);
            sumDays.addAndGet(curTime);
            sumErrorsPerTime.updateAndGet(v -> v + curErrors);
            sumSquareDays.addAndGet((int) Math.pow(curTime, 2));
        });

        double numerator = Consts.CURRENT_DAYS_COUNT * diminutiveInNumerator.get() - sumDays.get() * sumErrorsPerTime.get();
        double denominator = Consts.CURRENT_DAYS_COUNT * sumSquareDays.get() - Math.pow(sumDays.get(), 2);

        return numerator/denominator;
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
        int predictableDaysCountForDebuggingCompletion = Consts.CURRENT_DAYS_COUNT;

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
    public static void createReport(Map<Integer, Integer> inputValues, double probabilityGoal){
        // Excel file
        HSSFWorkbook workbook = new HSSFWorkbook();

        calculateKAndGatherStatisticTable(workbook, inputValues);
        createErrorsPerTimeGraphTable(workbook, inputValues);
        createLSMTable(workbook, inputValues);
        createLSMTableExtended(workbook, inputValues, 12);
        createLinearInterpolationTable(workbook,inputValues);
        createDebuggingCompletionPredictionTable(workbook, Consts.NO_ERRORS_PROBABILITY_GOAL);

        // Save excel
        try (FileOutputStream out = new FileOutputStream(new File(PathConsts.PATH_TO_SAVE_REPORT_FILE))) {
            workbook.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Excel saved!");
    }
}
