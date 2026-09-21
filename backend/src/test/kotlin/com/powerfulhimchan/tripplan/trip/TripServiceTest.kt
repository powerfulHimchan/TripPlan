package com.powerfulhimchan.tripplan.trip

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.*

@SpringBootTest
@Transactional
class TripServiceTest @Autowired constructor(private val service: TripService) {
    @Test
    fun `여행과 일정을 생성한다`() {
        val trip = service.create("user-1", CreateTripRequest("제주 여행", "제주", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 3), "Asia/Seoul"))
        val item = service.addItem("user-1", trip.id, CreateItineraryItemRequest("공항 도착", "제주공항", scheduledAt = Instant.parse("2027-05-01T01:00:00Z")))
        assertThat(item.endsAt).isEqualTo(Instant.parse("2027-05-01T02:00:00Z"))
        assertThat(service.get("user-1", trip.id).items).containsExactly(item)
    }

    @Test
    fun `일정 종료 시각은 시작 시각보다 늦어야 한다`() {
        val trip = service.create("user-1", CreateTripRequest("부산 여행", "부산", LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 2), "Asia/Seoul"))

        assertThatThrownBy {
            service.addItem(
                "user-1",
                trip.id,
                CreateItineraryItemRequest(
                    title = "광안리",
                    scheduledAt = Instant.parse("2027-06-01T03:00:00Z"),
                    endsAt = Instant.parse("2027-06-01T02:00:00Z"),
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("종료 시각")
    }

    @Test
    fun `종료일이 시작일보다 빠르면 실패한다`() {
        assertThatThrownBy {
            service.create("user-1", CreateTripRequest("잘못된 여행", "서울", LocalDate.of(2027, 5, 2), LocalDate.of(2027, 5, 1)))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `종료된 여행에는 일정을 추가할 수 없다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val trip = service.create(
            "user-1",
            CreateTripRequest("끝난 여행", "서울", today.minusDays(3), today.minusDays(1), "UTC"),
        )

        assertThatThrownBy {
            service.addItem(
                "user-1",
                trip.id,
                CreateItineraryItemRequest(
                    "뒤늦게 추가한 일정",
                    scheduledAt = today.minusDays(2).atTime(10, 0).toInstant(ZoneOffset.UTC),
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("종료된 여행")
    }
}
