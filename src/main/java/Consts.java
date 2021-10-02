import com.google.common.collect.ImmutableMap;
import java.util.Map;

public interface Consts {

    double K = 0.25;
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

    int MAX_PREDICTABLE_DAYS_COUNT_FOR_DEBUGGING_COMPLETION_BORDER = 500;
}
