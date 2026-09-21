package com.powerfulhimchan.tripplan.trip

import com.powerfulhimchan.tripplan.review.ReviewService
import com.powerfulhimchan.tripplan.review.TripOverallReviewService
import com.powerfulhimchan.tripplan.sharing.SharingService
import org.springframework.stereotype.Service

@Service
class TripDetailService(
    private val trips: TripService,
    private val reviews: ReviewService,
    private val overallReviews: TripOverallReviewService,
    private val sharing: SharingService,
) {
    fun get(userId: String, tripId: java.util.UUID) = TripDetailResponse(
        trip = trips.get(userId, tripId),
        reviews = reviews.getAllForTrip(userId, tripId),
        overallReview = overallReviews.get(userId, tripId),
        members = sharing.members(userId, tripId),
    )
}
