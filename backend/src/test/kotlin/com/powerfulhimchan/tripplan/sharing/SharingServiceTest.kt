package com.powerfulhimchan.tripplan.sharing

import com.powerfulhimchan.tripplan.auth.*
import com.powerfulhimchan.tripplan.trip.CreateTripRequest
import com.powerfulhimchan.tripplan.trip.TripService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@Transactional
class SharingServiceTest @Autowired constructor(
    private val authService: AuthService,
    private val users: AppUserRepository,
    private val tripService: TripService,
    private val sharingService: SharingService,
) {
    @Test
    fun `가입 회원이 여행 초대를 수락하면 공유 여행을 조회한다`() {
        val owner = authService.register(EmailPasswordRequest("owner@example.com", "password1"))
        val guest = authService.register(EmailPasswordRequest("guest@example.com", "password2"))
        val trip = tripService.create(
            owner.userId,
            CreateTripRequest("함께 가는 여행", "부산", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)),
        )

        val invitation = sharingService.invite(owner.userId, trip.id, InviteRequest(guest.email))
        assertThat(tripService.list(guest.userId)).isEmpty()

        sharingService.accept(guest.userId, invitation.id)

        assertThat(tripService.list(guest.userId).map { it.title }).containsExactly("함께 가는 여행")
        assertThat(sharingService.members(owner.userId, trip.id)).hasSize(2)
        assertThat(users.findByEmail(owner.email)?.passwordHash).doesNotContain("password1")
    }
}
