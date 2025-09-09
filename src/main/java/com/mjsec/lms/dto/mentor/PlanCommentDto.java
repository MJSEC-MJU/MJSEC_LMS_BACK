package com.mjsec.lms.dto.mentor;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCommentDto {

    @NotBlank(message = "댓글을 입력해주세요!")
    private String content;
}
