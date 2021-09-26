import java.util.LinkedHashMap;
import java.util.Map;

public class DjMorand {

    Map<Integer, Integer> inputValues = new LinkedHashMap<>();

    public DjMorand() {
        inputValues.put(1, 5);
        inputValues.put(2, 3);
        inputValues.put(3, 1);
        inputValues.put(4, 2);
        inputValues.put(5, 0);
        inputValues.put(6, 0);
        inputValues.put(7, 0);
    }

    public static void main(String[] args) {
        DjMorand test = new DjMorand();

        Calculator.calculateK(test.inputValues);
    }
}
