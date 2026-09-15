package com.powerfulhimchan.tripplan.review

import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
class ReviewPhotoStorage(@Value("\${tripplan.review.upload-dir:uploads/reviews}") uploadDir: String) {
    private val root: Path = Path.of(uploadDir).toAbsolutePath().normalize().also(Files::createDirectories)

    fun store(storedName: String, file: MultipartFile) {
        val target = root.resolve(storedName).normalize()
        require(target.parent == root) { "올바르지 않은 사진 경로입니다." }
        file.inputStream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun load(storedName: String): Resource {
        val target = root.resolve(storedName).normalize()
        if (target.parent != root || !Files.isRegularFile(target)) {
            throw EntityNotFoundException("사진 파일을 찾을 수 없습니다.")
        }
        return UrlResource(target.toUri())
    }

    fun delete(storedName: String) {
        val target = root.resolve(storedName).normalize()
        if (target.parent == root) Files.deleteIfExists(target)
    }
}
