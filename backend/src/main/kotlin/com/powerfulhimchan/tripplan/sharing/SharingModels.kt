package com.powerfulhimchan.tripplan.sharing

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "trip_members", uniqueConstraints = [UniqueConstraint(name = "uk_trip_member", columnNames = ["trip_id", "user_id"])])
class TripMember(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "trip_id", nullable = false) val tripId: UUID,
    @Column(name = "user_id", nullable = false, length = 36) val userId: String,
    @Column(name = "joined_at", nullable = false) val joinedAt: Instant = Instant.now(),
) {
    protected constructor() : this(tripId = UUID.randomUUID(), userId = "")
}

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED }

@Entity
@Table(name = "trip_invitations")
class TripInvitation(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "trip_id", nullable = false) val tripId: UUID,
    @Column(name = "inviter_id", nullable = false, length = 36) val inviterId: String,
    @Column(name = "invitee_id", nullable = false, length = 36) val inviteeId: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: InvitationStatus = InvitationStatus.PENDING,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "responded_at") var respondedAt: Instant? = null,
) {
    protected constructor() : this(tripId = UUID.randomUUID(), inviterId = "", inviteeId = "")
}

interface TripMemberRepository : org.springframework.data.jpa.repository.JpaRepository<TripMember, UUID> {
    fun existsByTripIdAndUserId(tripId: UUID, userId: String): Boolean
    fun findAllByTripId(tripId: UUID): List<TripMember>
}

interface TripInvitationRepository : org.springframework.data.jpa.repository.JpaRepository<TripInvitation, UUID> {
    fun existsByTripIdAndInviteeIdAndStatus(tripId: UUID, inviteeId: String, status: InvitationStatus): Boolean
    fun findAllByInviteeIdAndStatusOrderByCreatedAtDesc(inviteeId: String, status: InvitationStatus): List<TripInvitation>
    fun findByIdAndInviteeId(id: UUID, inviteeId: String): TripInvitation?
}

data class InviteRequest(
    @field:Email(message = "올바른 이메일 주소를 입력해주세요.")
    @field:NotBlank(message = "초대할 이메일 주소를 입력해주세요.")
    val email: String,
)

data class InvitationResponse(
    val id: UUID,
    val tripId: UUID,
    val tripTitle: String,
    val inviterEmail: String,
    val inviteeEmail: String,
    val status: InvitationStatus,
    val createdAt: Instant,
)

data class TripMemberResponse(val userId: String, val email: String, val owner: Boolean)
