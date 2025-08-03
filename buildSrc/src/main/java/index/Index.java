package index;

import java.util.LinkedHashMap;
import java.util.Map;

public class Index<Version> {

    public final Map<Version, VersionIndex> versions = new LinkedHashMap<>();

}
