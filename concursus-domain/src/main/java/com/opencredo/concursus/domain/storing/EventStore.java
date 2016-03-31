package com.opencredo.concursus.domain.storing;

import com.opencredo.concursus.domain.events.Event;
import com.opencredo.concursus.domain.events.sourcing.EventRetriever;
import com.opencredo.concursus.domain.persisting.EventPersister;

/**
 * A store of {@link Event}s, into which they can be persisted and from which they can be retrieved.
 */
public interface EventStore extends EventPersister, EventRetriever {
}
