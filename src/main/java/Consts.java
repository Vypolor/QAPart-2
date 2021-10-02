import com.google.common.collect.ImmutableMap;
import java.util.Map;

public interface Consts {

    double START_K = 0.25;
    double EPSILON = 0.001;

    Map<Integer, Integer> ERRORS_PER_INTERVAL_MAP = ImmutableMap.<Integer, Integer>builder()
            .put(1, 5)
            .put(2, 5)
            .put(3, 1)
            .put(4, 1)
            .put(5, 1)
            .put(6, 1)
            .put(7, 0)
            .build();

    int CURRENT_DAYS_COUNT = ERRORS_PER_INTERVAL_MAP.size();
    double NO_ERRORS_PROBABILITY_GOAL = 0.999;
    int MAX_PREDICTABLE_DAYS_COUNT_FOR_DEBUGGING_COMPLETION_BORDER = 100;
}
