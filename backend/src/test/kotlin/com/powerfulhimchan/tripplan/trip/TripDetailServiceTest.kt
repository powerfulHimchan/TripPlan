package com.powerfulhimchan.tripplan.trip

import com.powerfulhimchan.tripplan.auth.AuthService
import com.powerfulhimchan.tripplan.auth.EmailPasswordRequest
import com.powerfulhimchan.tripplan.review.ReviewService
import com.powerfulhimchan.tripplan.review.SaveReviewRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@SpringBootTest
@Transactional
class TripDetailServiceTest @Autowired constructor(
    private val authService: AuthService,
    private val tripService: TripService,
    private val reviewService: ReviewService,
    private val detailService: TripDetailService,
) {
    @Test
    fun `여행 상세에서 종료된 일정의 후기와 멤버를 한 번에 조회한다`() {
        val owner = authService.register(EmailPasswordRequest("detail-owner@example.com", "password1"))
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = Instant.now()
        val trip = tripService.create(
            owner.userId,
            CreateTripRequest("서울 하루 여행", "서울", today.minusDays(1), today.plusDays(1), "UTC"),
        )
        val completedItem = tripService.addItem(
            owner.userId,
            trip.id,
            CreateItineraryItemRequest(
                title = "아침 산책",
                scheduledAt = now.minusSeconds(7_200),
                endsAt = now.minusSeconds(3_600),
            ),
        )
        tripService.addItem(
            owner.userId,
            trip.id,
            CreateItineraryItemRequest(
                title = "저녁 식사",
                scheduledAt = now.plusSeconds(3_600),
                endsAt = now.plusSeconds(7_200),
            ),
        )
        reviewService.save(owner.userId, completedItem.id, SaveReviewRequest(5, "기분 좋은 시작"))

        val detail = detailService.get(owner.userId, trip.id)

        assertThat(detail.trip.items).hasSize(2)
        assertThat(detail.reviews.map { it.itemId }).containsExactly(completedItem.id)
        assertThat(detail.reviews.single().content).isEqualTo("기분 좋은 시작")
        val member = detail.members.single()
        assertThat(member.email).isEqualTo(owner.email)
        assertThat(member.owner).isTrue()
        assertThat(detail.overallReview).isNull()
    }
}
