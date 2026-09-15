package com.powerfulhimchan.tripplan.common

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val code: String, val message: String)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(EntityNotFoundException::class)
    fun notFound(e: EntityNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError("NOT_FOUND", e.message ?: "찾을 수 없습니다."))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException) =
        ResponseEntity.badRequest().body(ApiError("BAD_REQUEST", e.message ?: "잘못된 요청입니다."))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "입력값을 확인해주세요."
        return ResponseEntity.badRequest().body(ApiError("VALIDATION_ERROR", message))
    }
}

