package index;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class VersionIndex {

    public final Map<String, Set<String>> added = new LinkedHashMap<>();

    public final Map<String, Set<String>> removed = new LinkedHashMap<>();

}
