package com.hotel.dto.review.incentive;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewLeaderboardDTO {
    private String periodType; // monthly, quarterly, yearly
    private String period; // YYYY-MM for monthly, YYYY-QQ for quarterly, YYYY for yearly
    private LocalDateTime updatedAt;
    private List<LeaderboardEntryDTO> entries;

    @Data
    public static class LeaderboardEntryDTO {
        private Long userId;
        private String userName;
        private String userAvatar;
        private Integer rank;
        private Integer totalReviews;
        private Integer qualityScore;
        private Integer totalPoints;
        private Boolean isCurrentUser;
    }
}