package ewm.location.service;

import ewm.location.model.*;
import ewm.event.model.Event;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LocationSpecifications {
    public static Specification<Location> withTextContains(String text) {
        if (text == null || text.isBlank())
            return null;

        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + text.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("address")), "%" + text.toLowerCase() + "%")
                );
    }

    public static Specification<Location> withCreator(Long user) {
        if (user == null)
            return null;

        if (user == 0) {
            return (root, query, cb) ->
                    cb.isNull(root.get("creator").get("id"));
        }

        return (root, query, cb) ->
                cb.equal(root.get("creator").get("id"), user);
    }

    public static Specification<Location> withState(LocationState state) {
        if (state == null)
            return null;

        return (root, query, cb) ->
                cb.equal(root.get("state"), state);
    }

    public static Specification<Location> withEventsCount(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) {
                return null;
            }

            assert query != null;
            Subquery<Long> eventsSubquery = query.subquery(Long.class);
            Root<Event> eventRoot = eventsSubquery.from(Event.class);
            eventsSubquery.select(cb.count(eventRoot));
            eventsSubquery.where(cb.equal(eventRoot.get("location"), root));

            Predicate predicate = null;

            if (min != null) {
                predicate = cb.greaterThanOrEqualTo(eventsSubquery, min.longValue());
            }

            if (max != null) {
                Predicate maxPredicate = cb.lessThanOrEqualTo(eventsSubquery, max.longValue());
                predicate = predicate == null
                        ? maxPredicate
                        : cb.and(predicate, maxPredicate);
            }
            return predicate;
        };
    }

    public static Specification<Location> withCoordinates(Zone zone) {
        if (zone == null)
            return null;

        return (root, query, cb) -> {
            Expression<Double> distance = cb.function(
                    "calc_dist",
                    Double.class,
                    cb.literal(zone.getLatitude()),
                    cb.literal(zone.getLongitude()),
                    root.get("latitude"),
                    root.get("longitude")
            );
            return cb.lessThanOrEqualTo(distance, zone.getRadius());
        };
    }
}

