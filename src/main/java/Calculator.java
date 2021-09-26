import java.util.Map;

public class Calculator {

    public static Double calculateK (Map<Integer, Integer> inputValues){
        double k = Consts.K;

        double leftSum = 0.0;
        double rightSum = 0.0;
        double deltaSum = 0.0;

        double iterNumber = 0.0;
        do {

            ++iterNumber;
            leftSum = calculateLeftSum(inputValues, k);
            rightSum = calculateRightSum(inputValues, k);

            deltaSum = leftSum - rightSum;
            k = changeKValue(leftSum, rightSum, k, iterNumber);
        } while (Math.abs(deltaSum) > Consts.EPSILON);

        return k;
    }

    private static double calculateLeftSum(Map<Integer, Integer> inputValues, double k){

        double sum = 0.0;

        for (Map.Entry<Integer, Integer> current : inputValues.entrySet()) {

            double exp = Math.pow(Math.E, k * current.getKey() * (-1));
            sum += current.getValue() * current.getKey() * exp;
        }

        return sum;
    }

    private static double calculateRightSum(Map<Integer, Integer> inputValues, double k){

        double sumNumerator1 = 0.0;
        double sumNumerator2 = 0.0;
        double sumDenominator = 0.0;

        for (Map.Entry<Integer, Integer> current : inputValues.entrySet()) {
            double exp1 = Math.pow(Math.E, k * current.getKey() * (-1));
            double exp2 = Math.pow(Math.E, k * current.getKey() * (-2));

            sumNumerator1 += current.getValue() * exp1;
            sumNumerator2 += current.getKey() * exp2;
            sumDenominator += exp2;
        }

        return (sumNumerator1 * sumNumerator2) / sumDenominator;
    }

    private static double changeKValue(double leftSum, double rightSum, double k, double iterNumber){
        if(rightSum > leftSum){
            return (k * 2.0)/iterNumber ;
        }
        else {
            return (k / 2)/iterNumber;
        }
    }
}
