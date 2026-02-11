package uk.ac.warwick.dcs.sherlock.api.model.preprocessing;

import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.util.IPreprocessArtifact;
import java.util.List;

public record LineListArtifact(
        List<IndexedString> lines
) implements IPreprocessArtifact {}