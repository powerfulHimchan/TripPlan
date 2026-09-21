package com.powerfulhimchan.tripplan.review

import jakarta.validation.Valid
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.util.UUID

@RestController
@RequestMapping("/api/v1/items/{itemId}/review")
class ReviewController(private val service: ReviewService) {
    @PutMapping
    fun save(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID,
             @Valid @RequestBody request: SaveReviewRequest) = service.save(jwt.subject, itemId, request)

    @GetMapping
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID): ResponseEntity<ReviewResponse> =
        service.get(jwt.subject, itemId)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @PostMapping("/photos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addPhotos(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable itemId: UUID,
        @RequestPart("files") files: List<MultipartFile>,
    ) = service.addPhotos(jwt.subject, itemId, files)

    @GetMapping("/photos/{photoId}/content")
    fun download(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable itemId: UUID,
        @PathVariable photoId: UUID,
    ): ResponseEntity<org.springframework.core.io.Resource> {
        val photo = service.download(jwt.subject, itemId, photoId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .contentLength(photo.sizeBytes)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(photo.originalName, StandardCharsets.UTF_8).build().toString(),
            )
            .body(photo.resource)
    }

    @DeleteMapping("/photos/{photoId}")
    fun deletePhoto(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable itemId: UUID,
        @PathVariable photoId: UUID,
    ) = service.deletePhoto(jwt.subject, itemId, photoId)
}
