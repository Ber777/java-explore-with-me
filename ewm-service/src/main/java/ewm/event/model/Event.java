package ewm.event.model;

import ewm.user.model.User;
import ewm.category.model.Category;
import ewm.location.model.Location;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User initiator;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Builder.Default
    @Column(nullable = false)
    private Boolean paid = false;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Builder.Default
    @Column(name = "participant_limit")
    private Integer participantLimit = 0;

    @Builder.Default
    @Column(name = "request_moderation")
    private Boolean requestModeration = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventState state = EventState.PENDING;

    @Transient
    @Builder.Default
    private Integer confirmedRequests = 0;

    @Transient
    @Builder.Default
    private Integer views = 0;
}
