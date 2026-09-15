package com.powerfulhimchan.tripplan.sharing

import com.powerfulhimchan.tripplan.auth.AppUserRepository
import com.powerfulhimchan.tripplan.trip.TripRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SharingService(
    private val trips: TripRepository,
    private val users: AppUserRepository,
    private val invitations: TripInvitationRepository,
    private val members: TripMemberRepository,
) {
    @Transactional
    fun invite(ownerId: String, tripId: UUID, request: InviteRequest): InvitationResponse {
        val trip = trips.findByIdAndUserId(tripId, ownerId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        val invitee = users.findByEmail(request.email.trim().lowercase())
            ?: throw IllegalArgumentException("가입되지 않은 이메일입니다.")
        require(invitee.id != ownerId) { "본인은 초대할 수 없습니다." }
        require(!members.existsByTripIdAndUserId(tripId, invitee.id)) { "이미 공유 중인 회원입니다." }
        require(!invitations.existsByTripIdAndInviteeIdAndStatus(tripId, invitee.id, InvitationStatus.PENDING)) {
            "이미 초대를 보낸 회원입니다."
        }
        val invitation = invitations.save(TripInvitation(tripId = tripId, inviterId = ownerId, inviteeId = invitee.id))
        return invitation.toResponse(trip.title)
    }

    @Transactional(readOnly = true)
    fun received(userId: String): List<InvitationResponse> =
        invitations.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(userId, InvitationStatus.PENDING).map { invitation ->
            val trip = trips.findById(invitation.tripId).orElseThrow { EntityNotFoundException("여행을 찾을 수 없습니다.") }
            invitation.toResponse(trip.title)
        }

    @Transactional
    fun accept(userId: String, invitationId: UUID): InvitationResponse {
        val invitation = pendingInvitation(userId, invitationId)
        invitation.status = InvitationStatus.ACCEPTED
        invitation.respondedAt = Instant.now()
        if (!members.existsByTripIdAndUserId(invitation.tripId, userId)) {
            members.save(TripMember(tripId = invitation.tripId, userId = userId))
        }
        val trip = trips.findById(invitation.tripId).orElseThrow { EntityNotFoundException("여행을 찾을 수 없습니다.") }
        return invitation.toResponse(trip.title)
    }

    @Transactional
    fun decline(userId: String, invitationId: UUID): InvitationResponse {
        val invitation = pendingInvitation(userId, invitationId)
        invitation.status = InvitationStatus.DECLINED
        invitation.respondedAt = Instant.now()
        val trip = trips.findById(invitation.tripId).orElseThrow { EntityNotFoundException("여행을 찾을 수 없습니다.") }
        return invitation.toResponse(trip.title)
    }

    @Transactional(readOnly = true)
    fun members(requesterId: String, tripId: UUID): List<TripMemberResponse> {
        val trip = trips.findAccessible(tripId, requesterId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        val owner = users.findById(trip.userId).orElseThrow { EntityNotFoundException("여행 소유자를 찾을 수 없습니다.") }
        return listOf(TripMemberResponse(owner.id, owner.email, true)) + members.findAllByTripId(tripId).map { member ->
            val user = users.findById(member.userId).orElseThrow { EntityNotFoundException("회원을 찾을 수 없습니다.") }
            TripMemberResponse(user.id, user.email, false)
        }
    }

    private fun pendingInvitation(userId: String, invitationId: UUID): TripInvitation {
        val invitation = invitations.findByIdAndInviteeId(invitationId, userId)
            ?: throw EntityNotFoundException("초대를 찾을 수 없습니다.")
        require(invitation.status == InvitationStatus.PENDING) { "이미 처리된 초대입니다." }
        return invitation
    }

    private fun TripInvitation.toResponse(tripTitle: String): InvitationResponse {
        val inviter = users.findById(inviterId).orElseThrow { EntityNotFoundException("초대한 회원을 찾을 수 없습니다.") }
        val invitee = users.findById(inviteeId).orElseThrow { EntityNotFoundException("초대받은 회원을 찾을 수 없습니다.") }
        return InvitationResponse(id, tripId, tripTitle, inviter.email, invitee.email, status, createdAt)
    }
}

