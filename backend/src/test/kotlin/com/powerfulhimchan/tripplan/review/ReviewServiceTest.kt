package com.powerfulhimchan.tripplan.review

import com.powerfulhimchan.tripplan.trip.CreateItineraryItemRequest
import com.powerfulhimchan.tripplan.trip.CreateTripRequest
import com.powerfulhimchan.tripplan.trip.TripService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.springframework.mock.web.MockMultipartFile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@SpringBootTest
@Transactional
class ReviewServiceTest @Autowired constructor(
    private val tripService: TripService,
    private val reviewService: ReviewService,
) {
    @Test
    fun `각 계획에 서로 다른 후기를 저장한다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = Instant.now()
        val trip = tripService.create(
            "user-1",
            CreateTripRequest("진행 중인 제주 여행", "제주", today.minusDays(1), today.plusDays(1), "UTC"),
        )
        val first = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest(
                "제주공항",
                scheduledAt = now.minusSeconds(14_400),
                endsAt = now.minusSeconds(10_800),
            ),
        )
        val second = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest(
                "성산일출봉",
                scheduledAt = now.minusSeconds(7_200),
                endsAt = now.minusSeconds(3_600),
            ),
        )

        reviewService.save("user-1", first.id, SaveReviewRequest(4, "동선이 편했다."))
        reviewService.addPhotos(
            "user-1",
            first.id,
            listOf(MockMultipartFile("files", "airport.jpg", "image/jpeg", "photo-data".toByteArray())),
        )
        reviewService.save("user-1", second.id, SaveReviewRequest(5, "경치가 좋았다."))

        assertThat(reviewService.get("user-1", first.id)?.content).isEqualTo("동선이 편했다.")
        assertThat(reviewService.get("user-1", first.id)?.photos).hasSize(1)
        assertThat(reviewService.get("user-1", second.id)?.content).isEqualTo("경치가 좋았다.")
    }

    @Test
    fun `후기 사진은 다섯 장을 초과할 수 없다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = Instant.now()
        val trip = tripService.create(
            "user-1",
            CreateTripRequest("진행 중인 부산 여행", "부산", today.minusDays(1), today.plusDays(1), "UTC"),
        )
        val item = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest(
                "광안리",
                scheduledAt = now.minusSeconds(7_200),
                endsAt = now.minusSeconds(3_600),
            ),
        )
        reviewService.save("user-1", item.id, SaveReviewRequest(5, "야경이 좋았다."))
        val sixPhotos = (1..6).map {
            MockMultipartFile("files", "photo-$it.jpg", "image/jpeg", "photo-$it".toByteArray())
        }

        assertThatThrownBy { reviewService.addPhotos("user-1", item.id, sixPhotos) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("최대 5장")
    }

    @Test
    fun `여행이 진행 중이어도 종료된 일정에는 후기를 작성할 수 있다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val trip = tripService.create(
            "user-1",
            CreateTripRequest("진행 중인 여행", "서울", today.minusDays(1), today.plusDays(1), "UTC"),
        )
        val item = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest(
                "지난 일정",
                scheduledAt = Instant.now().minusSeconds(7_200),
                endsAt = Instant.now().minusSeconds(3_600),
            ),
        )

        val review = reviewService.save("user-1", item.id, SaveReviewRequest(5, "여행 중 바로 남긴 후기"))

        assertThat(review.content).isEqualTo("여행 중 바로 남긴 후기")
    }

    @Test
    fun `종료되지 않은 일정에는 후기를 작성할 수 없다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val trip = tripService.create(
            "user-1",
            CreateTripRequest("진행 중인 여행", "서울", today.minusDays(1), today.plusDays(1), "UTC"),
        )
        val item = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest(
                "다가오는 일정",
                scheduledAt = Instant.now().plusSeconds(3_600),
                endsAt = Instant.now().plusSeconds(7_200),
            ),
        )

        assertThatThrownBy { reviewService.save("user-1", item.id, SaveReviewRequest(5, "미리 쓴 후기")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("일정 종료 후")
    }

    @Test
    fun `별점만 있는 후기도 저장할 수 있다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val trip = tripService.create(
            "user-1",
            CreateTripRequest("후기 여행", "서울", today.minusDays(1), today.plusDays(1), "UTC"),
        )
        val item = tripService.addItem(
            "user-1", trip.id,
            CreateItineraryItemRequest(
                "지난 일정",
                scheduledAt = Instant.now().minusSeconds(7_200),
                endsAt = Instant.now().minusSeconds(3_600),
            ),
        )

        val review = reviewService.save("user-1", item.id, SaveReviewRequest(4, ""))

        assertThat(review.rating).isEqualTo(4)
        assertThat(review.content).isEmpty()
    }
}
