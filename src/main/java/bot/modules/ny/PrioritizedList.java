package bot.modules.ny;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PrioritizedList<T> {
    private final HashMap<T, Float> priorityMap;
    private float priorityLength = 0f;
    private static final Random random = new Random();

    public PrioritizedList(HashMap<T, Float> priorityMap) {
        this.priorityMap = priorityMap;
        for (float priority :
                priorityMap.values()) {
            priorityLength += priority;
        }
    }

    public HashMap<T, Float> getPriorityMap() {
        return priorityMap;
    }

    public T randomPick() {
        float pickNumber = random.nextFloat() * priorityLength;
        float i = 0;
        T picked = null;

        for (Map.Entry<T, Float> entry : priorityMap.entrySet()) {
            i += entry.getValue();

            if (i >= pickNumber) {
                picked = entry.getKey();
                break;
            }
        }

        return picked;
    }

    public T randomPickFromKeys(List<T> keys) {
        HashMap<T, Float> tempMap = new HashMap<>();
        for (T key : keys) {
            if (!priorityMap.containsKey(key)) {
                continue;
            }

            tempMap.put(key, priorityMap.get(key));
        }
        PrioritizedList<T> tempPrioritizedList = new PrioritizedList<>(tempMap);
        return tempPrioritizedList.randomPick();
    }
}
