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
        val item = service.addItem(
            "user-1",
            trip.id,
            CreateItineraryItemRequest(
                "공항 도착",
                "제주공항",
                scheduledAt = Instant.parse("2027-05-01T01:00:00Z"),
                category = ItineraryCategory.TRANSPORTATION,
                costWon = 25_000,
            ),
        )
        assertThat(item.endsAt).isEqualTo(Instant.parse("2027-05-01T02:00:00Z"))
        assertThat(item.category).isEqualTo(ItineraryCategory.TRANSPORTATION)
        assertThat(item.costWon).isEqualTo(25_000)
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
    fun `일정 비용은 음수일 수 없다`() {
        val trip = service.create(
            "user-1",
            CreateTripRequest("비용 여행", "서울", LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 2), "Asia/Seoul"),
        )

        assertThatThrownBy {
            service.addItem(
                "user-1",
                trip.id,
                CreateItineraryItemRequest(
                    title = "잘못된 비용",
                    scheduledAt = Instant.parse("2027-07-01T03:00:00Z"),
                    costWon = -1,
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("0원 이상")
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

    @Test
    fun `시작되지 않은 일정을 수정한다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = Instant.now()
        val trip = service.create(
            "user-1",
            CreateTripRequest("예정된 여행", "서울", today, today.plusDays(1), "UTC"),
        )
        val item = service.addItem(
            "user-1",
            trip.id,
            CreateItineraryItemRequest("기존 일정", scheduledAt = now.plusSeconds(7_200)),
        )

        val updated = service.updateItem(
            "user-1",
            item.id,
            UpdateItineraryItemRequest(
                title = "수정한 일정",
                place = "서울역",
                memo = "변경된 메모",
                scheduledAt = now.plusSeconds(10_800),
                endsAt = now.plusSeconds(14_400),
                notificationEnabled = false,
                notificationMinutesBefore = 10,
                category = ItineraryCategory.SIGHTSEEING,
                costWon = 120_000,
            ),
        )

        assertThat(updated.title).isEqualTo("수정한 일정")
        assertThat(updated.place).isEqualTo("서울역")
        assertThat(updated.endsAt).isEqualTo(now.plusSeconds(14_400))
        assertThat(updated.notificationEnabled).isFalse()
        assertThat(updated.category).isEqualTo(ItineraryCategory.SIGHTSEEING)
        assertThat(updated.costWon).isEqualTo(120_000)
    }

    @Test
    fun `이미 시작된 일정은 수정할 수 없다`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = Instant.now()
        val trip = service.create(
            "user-1",
            CreateTripRequest("진행 중인 여행", "서울", today.minusDays(1), today.plusDays(1), "UTC"),
        )
        val item = service.addItem(
            "user-1",
            trip.id,
            CreateItineraryItemRequest(
                "진행 중 일정",
                scheduledAt = now.minusSeconds(3_600),
                endsAt = now.plusSeconds(3_600),
            ),
        )

        assertThatThrownBy {
            service.updateItem(
                "user-1",
                item.id,
                UpdateItineraryItemRequest(
                    title = "수정 시도",
                    scheduledAt = now.plusSeconds(7_200),
                    endsAt = now.plusSeconds(10_800),
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("시작된 일정")
    }

    @Test
    fun `소유자는 여행 기본 정보를 수정한다`() {
        val trip = service.create(
            "owner-1",
            CreateTripRequest("기존 여행", "서울", LocalDate.of(2027, 8, 1), LocalDate.of(2027, 8, 3), "Asia/Seoul"),
        )

        val updated = service.update(
            "owner-1",
            trip.id,
            UpdateTripRequest("수정한 여행", "부산", LocalDate.of(2027, 8, 1), LocalDate.of(2027, 8, 4), "Asia/Seoul"),
        )

        assertThat(updated.title).isEqualTo("수정한 여행")
        assertThat(updated.destination).isEqualTo("부산")
        assertThat(updated.endDate).isEqualTo(LocalDate.of(2027, 8, 4))
        assertThat(updated.owner).isTrue()
    }

    @Test
    fun `기존 일정이 변경 기간 밖이면 여행을 수정할 수 없다`() {
        val trip = service.create(
            "owner-2",
            CreateTripRequest("기간 여행", "서울", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 3), "UTC"),
        )
        service.addItem(
            "owner-2",
            trip.id,
            CreateItineraryItemRequest("마지막 일정", scheduledAt = Instant.parse("2027-09-03T10:00:00Z")),
        )

        assertThatThrownBy {
            service.update(
                "owner-2",
                trip.id,
                UpdateTripRequest("짧아진 여행", "서울", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 2), "UTC"),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("여행 기간 밖")
    }

    @Test
    fun `여행을 보관하고 다시 복원한다`() {
        val trip = service.create(
            "owner-3",
            CreateTripRequest("보관 여행", "제주", LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 2), "Asia/Seoul"),
        )

        service.archive("owner-3", trip.id)

        assertThat(service.list("owner-3")).isEmpty()
        assertThat(service.list("owner-3", archived = true).single().id).isEqualTo(trip.id)

        service.unarchive("owner-3", trip.id)

        assertThat(service.list("owner-3").single().id).isEqualTo(trip.id)
        assertThat(service.list("owner-3", archived = true)).isEmpty()
    }

    @Test
    fun `소유자는 여행을 삭제한다`() {
        val trip = service.create(
            "owner-4",
            CreateTripRequest("삭제 여행", "서울", LocalDate.of(2027, 11, 1), LocalDate.of(2027, 11, 2), "Asia/Seoul"),
        )

        service.delete("owner-4", trip.id)

        assertThat(service.list("owner-4")).isEmpty()
        assertThatThrownBy { service.get("owner-4", trip.id) }
            .isInstanceOf(jakarta.persistence.EntityNotFoundException::class.java)
    }
}
