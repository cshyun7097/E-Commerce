package study.seunghyun.ecommerce.global;

import lombok.Builder;

// code: 실패 시 ErrorCode 이름 (성공 시 null)
@Builder
public record ApiResponse<T>(boolean success, String code, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static ApiResponse<Void> ok() {
        return ApiResponse.<Void>builder().success(true).build();
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode.name(), errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return ApiResponse.<Void>builder().success(false).code(code).message(message).build();
    }
}
