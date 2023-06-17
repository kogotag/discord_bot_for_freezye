package bot.modules.ny;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NyProcessor {
    private float responseProbability = 1.0f;
    private final float probabilityGrowthTimeIncrement = 60f;
    private static final Random random = new Random();
    private final PrioritizedList<NyEventType> eventList;
    private final PrioritizedList<String> nyCleanResponses;
    private final PrioritizedList<String> nyContainsResponses;

    public NyProcessor() {
        HashMap<NyEventType, Float> eventPriorityMap = new HashMap<>();
        eventPriorityMap.put(NyEventType.NY_CLEAN, 5.0f);
        eventPriorityMap.put(NyEventType.NY_CONTAINS, 1.0f);
        eventList = new PrioritizedList<>(eventPriorityMap);

        HashMap<String, Float> nyCleanResponsePriorityMap = new HashMap<>();
        nyCleanResponsePriorityMap.put("баранки гну", 0.5f);
        nyCleanResponsePriorityMap.put("что ну", 1.0f);
        nyCleanResponsePriorityMap.put("типа ну типо", 1.0f);
        nyCleanResponsePriorityMap.put("ну что", 1.0f);
        nyCleanResponsePriorityMap.put("ну, погоди!", 1.0f);
        nyCleanResponsePriorityMap.put("без ну", 1.0f);
        nyCleanResponses = new PrioritizedList<>(nyCleanResponsePriorityMap);

        HashMap<String, Float> nyContainsResponsePriorityMap = new HashMap<>();
        nyContainsResponsePriorityMap.put("типа ну типо", 1.0f);
        nyContainsResponsePriorityMap.put("без ну", 1.0f);
        nyContainsResponses = new PrioritizedList<>(nyContainsResponsePriorityMap);
    }

    public String onEvent(String message) {
        float triggerNumber = random.nextFloat();
        if (triggerNumber > responseProbability) {
            return null;
        }

        List<NyEventType> triggeredEventTypes = new ArrayList<>();

        Matcher nyCleanMatcher = Pattern.compile("^ну[у\\.]*$").matcher(message);
        if (nyCleanMatcher.matches()) {
            triggeredEventTypes.add(NyEventType.NY_CLEAN);
        }

        Matcher nyContainsMatcher = Pattern.compile("(^ну)|(\\s+ну)").matcher(message);
        if (nyContainsMatcher.find()) {
            triggeredEventTypes.add(NyEventType.NY_CONTAINS);
        }

        NyEventType pickedEvent = eventList.randomPickFromKeys(triggeredEventTypes);

        String response = null;
        float aggressiveness = 0.0f;

        if (pickedEvent.equals(NyEventType.NY_CLEAN)) {
            response = nyCleanResponses.randomPick();
            aggressiveness = 30f;
        } else if (pickedEvent.equals(NyEventType.NY_CONTAINS)) {
            response = nyContainsResponses.randomPick();
            aggressiveness = 50f;
        }

        decreaseResponseProbability(aggressiveness);

        return response;
    }

    private void mapResponseProbability() {
        responseProbability = Math.min(Math.max(0f, responseProbability), 1f);
    }

    public void increaseResponseProbabilityByTime(long deltaTimeSeconds) {
        mapResponseProbability();

        if (responseProbability == 1f) {
            return;
        }

        float probabilityGrowthTime = (float) (-1f * probabilityGrowthTimeIncrement * Math.log(1f - responseProbability));
        responseProbability = (float) (1f - Math.exp(-1f * (probabilityGrowthTime + deltaTimeSeconds) / probabilityGrowthTimeIncrement));
    }

    private void decreaseResponseProbability(float lastResponseAggressiveness) {
        mapResponseProbability();
        float aggressiveness = Math.min(99.0f, lastResponseAggressiveness);
        responseProbability *= (1.0f - (aggressiveness / 100.0f));
    }
}
