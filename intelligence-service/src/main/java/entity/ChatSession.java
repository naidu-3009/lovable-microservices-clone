package entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
@Table(name = "chat_sessions")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatSession {

    @EmbeddedId
    private ChatSessionId chatSessionId;


    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;

    Instant deletedAt;
}
