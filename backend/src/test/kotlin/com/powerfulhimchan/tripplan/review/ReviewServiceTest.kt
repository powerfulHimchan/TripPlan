package com.powerfulhimchan.tripplan.review

import com.powerfulhimchan.tripplan.trip.CreateItineraryItemRequest
import com.powerfulhimchan.tripplan.trip.CreateTripRequest
import com.powerfulhimchan.tripplan.trip.TripService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@Transactional
class ReviewServiceTest @Autowired constructor(
    private val tripService: TripService,
    private val reviewService: ReviewService,
) {
    @Test
    fun `각 계획에 서로 다른 후기를 저장한다`() {
        val trip = tripService.create(
            "user-1",
            CreateTripRequest("지난 제주 여행", "제주", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 3)),
        )
        val first = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest("제주공항", scheduledAt = Instant.parse("2025-05-01T01:00:00Z")),
        )
        val second = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest("성산일출봉", scheduledAt = Instant.parse("2025-05-02T01:00:00Z")),
        )

        reviewService.save("user-1", first.id, SaveReviewRequest(4, "동선이 편했다."))
        reviewService.save("user-1", second.id, SaveReviewRequest(5, "경치가 좋았다."))

        assertThat(reviewService.get("user-1", first.id)?.content).isEqualTo("동선이 편했다.")
        assertThat(reviewService.get("user-1", second.id)?.content).isEqualTo("경치가 좋았다.")
    }
}
