package com.opencredo.concursus.mapping.events.methods.reflection.interpreting;

import com.opencredo.concursus.data.tuples.Tuple;
import com.opencredo.concursus.data.tuples.TupleKey;
import com.opencredo.concursus.data.tuples.TupleKeyValue;
import com.opencredo.concursus.data.tuples.TupleSchema;
import com.opencredo.concursus.domain.events.Event;
import com.opencredo.concursus.domain.events.EventType;
import com.opencredo.concursus.domain.time.StreamTimestamp;
import com.opencredo.concursus.mapping.events.methods.ordering.CausalOrdering;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class EventMethodMapping {

    public static Map<EventType, TupleSchema> getEventTypeMappings(Collection<? extends EventMethodMapping> typeMappings) {
        if (typeMappings.stream().map(EventMethodMapping::getEventType).distinct().count() != typeMappings.size()) {
            throw new IllegalStateException(String.format("Duplicate event types detected: %s", typeMappings.stream()
                    .map(EventMethodMapping::getEventType)
                    .map(EventType::toString)
                    .collect(toList())));
        }

        return typeMappings.stream()
                .collect(toMap(
                        EventMethodMapping::getEventType,
                        EventMethodMapping::getTupleSchema));
    }

    public static Comparator<Event> makeCausalOrdering(Collection<? extends EventMethodMapping> typeMappings) {
        return CausalOrdering.onEventTypes(typeMappings.stream().collect(toMap(EventMethodMapping::getEventType, EventMethodMapping::getCausalOrder)));
    }

    private final EventType eventType;
    private final TupleSchema tupleSchema;
    private final TupleKey[] tupleKeys;
    private final int causalOrder;
    private final EventMethodType eventMethodType;

    EventMethodMapping(EventType eventType, TupleSchema tupleSchema, TupleKey[] tupleKeys, int causalOrder, EventMethodType eventMethodType) {
        this.eventType = eventType;
        this.tupleSchema = tupleSchema;
        this.tupleKeys = tupleKeys;
        this.causalOrder = causalOrder;
        this.eventMethodType = eventMethodType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public TupleSchema getTupleSchema() {
        return tupleSchema;
    }

    public int getCausalOrder() {
        return causalOrder;
    }

    public Object[] mapEvent(Event event) {
        return eventMethodType.apply(event, tupleKeys);
    }

    public Event mapArguments(Object[] args) {
        checkNotNull(args, "args must not be null");
        checkArgument(args.length == tupleKeys.length + 2,
                "Expected %s args, received %s", tupleKeys.length +2, args.length);
        checkArgument(args[1] instanceof String, "second argument %s is not a String", args[1]);
        checkArgument(args[0] instanceof StreamTimestamp, "first argument %s is not a StreamTimestamp", args[0]);

        return eventType.makeEvent((String) args[1], (StreamTimestamp) args[0], makeTupleFromArgs(args));
    }

    private Tuple makeTupleFromArgs(Object[] args) {
        return tupleSchema.make(IntStream.range(0, tupleKeys.length)
                .mapToObj(getValueFrom(args))
                .toArray(TupleKeyValue[]::new));
    }

    @SuppressWarnings("unchecked")
    private IntFunction<TupleKeyValue> getValueFrom(Object[] args) {
        return i -> tupleKeys[i].of(args[i + 2]);
    }

}
