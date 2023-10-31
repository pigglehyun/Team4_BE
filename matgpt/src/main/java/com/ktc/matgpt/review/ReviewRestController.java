package com.ktc.matgpt.review;

import com.ktc.matgpt.aws.FileValidator;
import com.ktc.matgpt.exception.CustomException;
import com.ktc.matgpt.exception.ErrorCode;
import com.ktc.matgpt.review.dto.ReviewRequest;
import com.ktc.matgpt.review.dto.ReviewResponse;
import com.ktc.matgpt.aws.S3Service;
import com.ktc.matgpt.feature_review.utils.ApiUtils;
import com.ktc.matgpt.security.UserPrincipal;
import com.ktc.matgpt.store.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequestMapping("/stores")
@RequiredArgsConstructor
@RestController
public class ReviewRestController {
    private final ReviewService reviewService;
    private final StoreService storeService;
    private final S3Service s3Service;

    // 첫 번째 단계: 리뷰 임시 저장 및 Presigned URL 반환
    @PostMapping("/{storeId}/reviews/temp")
    public ResponseEntity<ApiUtils.ApiResult<?>> createTemporaryReview(@PathVariable Long storeId,
                                                                       @RequestBody ReviewRequest.SimpleCreateDTO requestDTO,
                                                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Long reviewId = reviewService.createTemporaryReview(userPrincipal.getId(),storeId, requestDTO);
            List<ReviewResponse.UploadS3DTO.PresignedUrlDTO> presignedUrls = reviewService.createPresignedUrls(reviewId, requestDTO);
            return ResponseEntity.ok(ApiUtils.success(new ReviewResponse.UploadS3DTO(reviewId, presignedUrls)));
        } catch (FileValidator.FileValidationException e) {
            return ResponseEntity.badRequest().body(ApiUtils.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiUtils.error("서버 내부 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }


    // 두 번째 단계: 이미지와 태그 정보를 포함하여 리뷰 완료
    @PostMapping("/{storeId}/complete/{reviewId}")
    public ResponseEntity<?> completeReview(@PathVariable Long storeId,Long reviewId,
                                            @RequestBody ReviewRequest.CreateCompleteDTO requestDTO) {
        try {
            reviewService.completeReviewUpload(storeId, reviewId, requestDTO);
            return ResponseEntity.ok(ApiUtils.success("리뷰가 성공적으로 완료되었습니다."));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REVIEW_PROCESS_ERROR);
        }
    }

    // 음식점 리뷰 목록 조회
    @GetMapping("")
    public ResponseEntity<?> findAllByStoreId(@PathVariable Long storeId,
                                              @RequestParam(defaultValue = "latest") String sortBy,
                                              @RequestParam(defaultValue = "6") Long cursorId,
                                              @RequestParam(defaultValue = "5.0") double cursorRating
    ) {
        List<ReviewResponse.FindAllByStoreIdDTO> responseDTOs = reviewService.findAllByStoreId(storeId, sortBy, cursorId, cursorRating);

        ApiUtils.ApiResult<?> apiResult = ApiUtils.success(responseDTOs);
        return ResponseEntity.ok(apiResult);
    }


    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> update(@PathVariable Long reviewId,
                                    @RequestBody @Valid ReviewRequest.UpdateDTO requestDTO,
                                    @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        reviewService.update(reviewId, userPrincipal.getId(), requestDTO);
        String msg = "review-" + reviewId + " updated";

        ApiUtils.ApiResult<?> apiResult = ApiUtils.success(msg);
        return ResponseEntity.ok(apiResult);
    }

    // 개별 리뷰 상세조회
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> findById(@PathVariable Long reviewId) {
        ReviewResponse.FindByReviewIdDTO responseDTO = reviewService.findDetailByReviewId(reviewId);

        ApiUtils.ApiResult<?> apiResult = ApiUtils.success(responseDTO);
        return ResponseEntity.ok(apiResult);
    }

    // TODO: s3 삭제 구현
    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> delete(@PathVariable Long reviewId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        reviewService.delete(reviewId, userPrincipal.getId());

        ApiUtils.ApiResult<?> apiResult = ApiUtils.success(null);
        return ResponseEntity.ok(apiResult);
    }
}