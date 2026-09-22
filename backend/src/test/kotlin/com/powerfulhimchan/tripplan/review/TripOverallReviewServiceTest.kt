package com.powerfulhimchan.tripplan.review

import com.powerfulhimchan.tripplan.trip.ItineraryItem
import com.powerfulhimchan.tripplan.trip.ItineraryItemRepository
import com.powerfulhimchan.tripplan.trip.Trip
import com.powerfulhimchan.tripplan.trip.TripRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@Transactional
class TripOverallReviewServiceTest @Autowired constructor(
    private val trips: TripRepository,
    private val items: ItineraryItemRepository,
    private val reviewService: ReviewService,
    private val service: TripOverallReviewService,
) {
    @Test
    fun `일정 후기 사진을 여행 대표 사진으로 선택한다`() {
        val trip = pastTrip("user-1", "지난 제주 여행")
        val item = items.save(
            ItineraryItem(
                tripId = trip.id,
                title = "성산일출봉",
                scheduledAt = Instant.now().minusSeconds(172_800),
                endsAt = Instant.now().minusSeconds(169_200),
            ),
        )
        reviewService.save("user-1", item.id, SaveReviewRequest(5, "즐거운 여행"))
        val itineraryReview = reviewService.addPhotos(
            "user-1",
            item.id,
            listOf(MockMultipartFile("files", "cover.jpg", "image/jpeg", "photo-data".toByteArray())),
        )

        val saved = service.save(
            "user-1",
            trip.id,
            SaveTripOverallReviewRequest(5, "다시 가고 싶은 여행", itineraryReview.photos.single().id),
        )

        assertThat(saved.content).isEqualTo("다시 가고 싶은 여행")
        assertThat(saved.representativePhoto?.itemId).isEqualTo(item.id)
        assertThat(service.get("user-1", trip.id)?.representativePhoto?.originalName).isEqualTo("cover.jpg")
        assertThat(service.getAll("user-1")).singleElement().satisfies { review ->
            assertThat(review.tripId).isEqualTo(trip.id)
            assertThat(review.representativePhoto?.itemId).isEqualTo(item.id)
        }
    }

    @Test
    fun `다른 여행의 사진은 대표 사진으로 선택할 수 없다`() {
        val firstTrip = pastTrip("user-1", "첫 여행")
        val secondTrip = pastTrip("user-1", "두 번째 여행")
        val secondItem = items.save(
            ItineraryItem(
                tripId = secondTrip.id,
                title = "다른 여행 일정",
                scheduledAt = Instant.now().minusSeconds(172_800),
                endsAt = Instant.now().minusSeconds(169_200),
            ),
        )
        reviewService.save("user-1", secondItem.id, SaveReviewRequest(4, "다른 여행"))
        val photo = reviewService.addPhotos(
            "user-1",
            secondItem.id,
            listOf(MockMultipartFile("files", "other.jpg", "image/jpeg", "photo-data".toByteArray())),
        ).photos.single()

        assertThatThrownBy {
            service.save("user-1", firstTrip.id, SaveTripOverallReviewRequest(5, "잘못된 선택", photo.id))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("해당 여행")
    }

    private fun pastTrip(userId: String, title: String) = trips.save(
        Trip(
            userId = userId,
            title = title,
            destination = "서울",
            startDate = LocalDate.now().minusDays(3),
            endDate = LocalDate.now().minusDays(1),
            timezone = "UTC",
        ),
    )
}
